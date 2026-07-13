import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface DocSection {
  id: number;
  title: string;
  description: string[];
  image: string;
}

@Component({
  selector: 'app-documentation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './documentation.component.html',
  styleUrl: './documentation.component.scss',
})
export class DocumentationComponent {
  sections: DocSection[] = [
    {
      id: 1,
      title: 'Manage Committees',
      description: [
        'Navigate to the main dashboard to gain complete oversight of all the active committees.',
        'Click on the three dots for various options related to a committee.',
      ],
      image: 'images/dashboard.png',
    },
    {
      id: 2,
      title: 'Create Committee',
      description: [
        'Use the sidebar or press Alt + C to create a new committee swiftly.',
        'Enter essential committee details, such as the objective, primary language, and coordinator.',
        'Select members and dynamically assign specific roles (double click to unselect).',
        'Reorder the members via three-bar button to establish their hierarchy and how they will appear in meeting minutes.',
      ],
      image: 'images/create-committee.png',
    },
    {
      id: 3,
      title: 'Create Meeting',
      description: [
        'Create a new meeting from the sidebar, the committee dropdown, or by pressing Alt + N.',
        'Define the context of the meeting by entering the date, time, title, and location.',
        'Add agendas and decisions. Double-click to remove an item',
        'Curate the attendee list from the left panel.(double click to unselect)',
      ],
      image: 'images/create-meeting.png',
    },
    {
      id: 4,
      title: 'Manage Committee and its meetings',
      description: [
        'Select any committee to access its meeting history dates and other data related to the committee.',
      ],
      image: 'images/meeting-dashboard.png',
    },
    {
      id: 5,
      title: 'Live Minute Editing',
      description: [
        'Draft and revise meeting minutes continuously in the live editor.',
        'Save your progression effortlessly',
      ],
      image: 'images/minute.png',
    },
    {
      id: 6,
      title: 'Export & Distribution',
      description: [
        'Finalize the drafted minutes.',
        'Reorganize participant ordering for the output document if necessary (Note: This reordering is not persisted in the backedn).',
        'Generate a standardized Word document export, or directly print finalized minutes from your browser.',
      ],
      image: 'images/minute-options.png',
    },
    {
      id: 7,
      title: 'Global Search & System Management',
      description: [
        'Utilize the pervasive Global Search Bar to locate specific decisions, agendas, committees, or member profiles rapidly.',
      ],
      image: 'images/search-bar.png',
    },
  ];
}
