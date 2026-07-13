package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.GlobalSearchResultDto;
import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.repository.AgendaRepository;
import com.sep.mmms_backend.repository.CommitteeRepository;
import com.sep.mmms_backend.repository.DecisionRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GlobalSearchService {
    CommitteeRepository committeeRepository;
    MemberRepository memberRepository;
    DecisionRepository decisionRepository;
    AgendaRepository agendaRepository;

    GlobalSearchService(CommitteeRepository committeeRepository, MemberRepository memberRepository, DecisionRepository decisionRepository, AgendaRepository agendaRepository) {
        this.committeeRepository = committeeRepository;
        this.memberRepository = memberRepository;
        this.decisionRepository = decisionRepository;
        this.agendaRepository = agendaRepository;
    }


    public GlobalSearchResultDto globalSearch(String input, String username) {
        List<Committee> committees =  committeeRepository.findAll(containsKeyword(input, "name", username));
        List<Member> members = memberRepository.findAll(containsKeywordTwoFields(input, "firstName", "lastName", username));
        List<Decision> decisions = decisionRepository.findAll(containsKeyword(input, "decision", username));
        List<Agenda> agendas = agendaRepository.findAll(containsKeyword(input, "agenda", username));
        return new GlobalSearchResultDto(committees, members, decisions, agendas);
    }

    //NOTE: <T> is inferred based on whom the specification is being passed to
    public static <T> Specification<T> containsKeyword(String input, String parameterName, String createdBy) {
        return(root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(input != null && !input.trim().isEmpty()) {
                String[] keywords = input.trim().split("\\s+");

                Predicate createdByPredicate = criteriaBuilder.equal(root.get("createdBy"), createdBy);
                for(String keyword : keywords) {
                    Predicate predicate1 = criteriaBuilder.like(criteriaBuilder.lower(root.get(parameterName)), "%" + keyword.toLowerCase() + "%");

                    Predicate finalPredicate = criteriaBuilder.and(predicate1, createdByPredicate);

                    predicates.add(finalPredicate);
                }
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static <T> Specification<T> containsKeywordTwoFields(String input, String parameterName1, String parameterName2, String createdBy) {
        return(root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(input != null && !input.trim().isEmpty()) {
                String[] keywords = input.trim().split("\\s+");

                Predicate createdByPredicate = criteriaBuilder.equal(root.get("createdBy"), createdBy);
                for(String keyword : keywords) {
                    Predicate predicate1 = criteriaBuilder.like(criteriaBuilder.lower(root.get(parameterName1)), "%" + keyword.toLowerCase() + "%");
                    Predicate predicate2 = criteriaBuilder.like(criteriaBuilder.lower(root.get(parameterName2)), "%" + keyword.toLowerCase() + "%");

                    Predicate finalPredicate = criteriaBuilder.and(
                            criteriaBuilder.or(predicate1, predicate2),
                            createdByPredicate
                    );

                    predicates.add(finalPredicate);
                }
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

}
