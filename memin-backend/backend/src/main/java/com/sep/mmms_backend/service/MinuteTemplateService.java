package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.MinuteTemplateDto;
import com.sep.mmms_backend.dto.MinuteTemplateSummaryDto;
import com.sep.mmms_backend.dto.MinuteTemplateUpdateDto;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.MinuteTemplate;
import com.sep.mmms_backend.enums.AppRole;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.repository.MinuteTemplateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MinuteTemplateService {
    private static final String DEFAULT_TEMPLATE_NAME = "Current template";

    private final MinuteTemplateRepository templateRepository;
    private final CommitteeService committeeService;
    private final AppUserService appUserService;

    public MinuteTemplateService(
            MinuteTemplateRepository templateRepository,
            CommitteeService committeeService,
            AppUserService appUserService) {
        this.templateRepository = templateRepository;
        this.committeeService = committeeService;
        this.appUserService = appUserService;
    }

    @Transactional(readOnly = true)
    public MinuteTemplateDto getWorkspace(int committeeId, String username) {
        Committee committee = committeeService.getCommitteeIfAccessible(committeeId, username);
        MinuteTemplateDto workspace = new MinuteTemplateDto(committee);
        List<MinuteTemplateSummaryDto> templates = templateRepository
                .findByCommitteeIdOrderByIdDesc(committeeId)
                .stream()
                .map(template -> new MinuteTemplateSummaryDto(
                        template,
                        template.getId().equals(committee.getActiveMinuteTemplateId())))
                .toList();
        workspace.savedTemplates = templates;
        return workspace;
    }

    @Transactional
    public MinuteTemplateDto saveTemplate(
            int committeeId,
            MinuteTemplateUpdateDto update,
            String username) {
        Committee committee = getWritableCommittee(committeeId, username);
        if (update == null || update.getMinuteTemplateHtml() == null
                || update.getMinuteTemplateHtml().isBlank()) {
            throw new IllegalOperationException("A minute template cannot be empty");
        }

        String name = update.getName() == null || update.getName().isBlank()
                ? DEFAULT_TEMPLATE_NAME
                : update.getName().trim();

        MinuteTemplate template;
        if (update.getTemplateId() == null) {
            if (templateRepository.existsByCommitteeIdAndNameIgnoreCase(committeeId, name)) {
                throw new IllegalOperationException("A template with that name already exists");
            }
            template = new MinuteTemplate();
            template.setCommittee(committee);
            template.setCreatedBy(username);
        } else {
            template = templateRepository.findByIdAndCommitteeId(update.getTemplateId(), committeeId)
                    .orElseThrow(() -> new IllegalOperationException("Minute template not found"));
            boolean nameChanged = !name.equalsIgnoreCase(template.getName());
            if (nameChanged && templateRepository.existsByCommitteeIdAndNameIgnoreCase(committeeId, name)) {
                throw new IllegalOperationException("A template with that name already exists");
            }
        }

        template.setName(name);
        template.setMinuteTemplateHtml(update.getMinuteTemplateHtml().trim());
        template.setMinuteLanguage(committee.getMinuteLanguage());
        try {
            template = templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalOperationException("A template with that name already exists");
        }

        activate(committee, template);
        return getWorkspace(committeeId, username);
    }

    @Transactional
    public MinuteTemplateDto activateTemplate(int committeeId, int templateId, String username) {
        Committee committee = getWritableCommittee(committeeId, username);
        MinuteTemplate template = templateRepository.findByIdAndCommitteeId(templateId, committeeId)
                .orElseThrow(() -> new IllegalOperationException("Minute template not found"));
        activate(committee, template);
        return getWorkspace(committeeId, username);
    }

    @Transactional
    public void deleteTemplate(int committeeId, int templateId, String username) {
        Committee committee = getWritableCommittee(committeeId, username);
        MinuteTemplate template = templateRepository.findByIdAndCommitteeId(templateId, committeeId)
                .orElseThrow(() -> new IllegalOperationException("Minute template not found"));
        boolean wasActive = template.getId().equals(committee.getActiveMinuteTemplateId());
        templateRepository.delete(template);

        if (wasActive) {
            MinuteTemplate replacement = templateRepository.findByCommitteeIdOrderByIdDesc(committeeId)
                    .stream()
                    .filter(candidate -> !candidate.getId().equals(templateId))
                    .findFirst()
                    .orElse(null);
            if (replacement == null) {
                committee.setActiveMinuteTemplateId(null);
                committee.setMinuteTemplateHtml(null);
            } else {
                activate(committee, replacement);
            }
        }
    }

    private void activate(Committee committee, MinuteTemplate template) {
        committee.setActiveMinuteTemplateId(template.getId());
        committee.setMinuteTemplateHtml(template.getMinuteTemplateHtml());
        // CommitteeService owns the repository used for normal committee
        // operations; saving through its public method is intentionally avoided
        // here because this service already has the managed entity instance.
        committeeService.saveTemplateActivation(committee);
    }

    private Committee getWritableCommittee(int committeeId, String username) {
        Committee committee = committeeService.getCommitteeIfAccessible(committeeId, username);
        AppUser user = appUserService.loadUserByUsername(username);
        boolean departmentHead = user.getRole() == AppRole.DEPARTMENT_HEAD;
        boolean creator = username.equals(committee.getCreatedBy());
        boolean secretary = user.getRole() == AppRole.SECRETARY
                && user.getLinkedMemberId() != null
                && committee.getSecretary() != null
                && user.getLinkedMemberId().equals(committee.getSecretary().getId());
        if (!departmentHead && !creator && !secretary) {
            throw new IllegalOperationException("Only the department head, committee creator, or assigned secretary can edit the minute template");
        }
        return committee;
    }
}
