Application ToDo Overview and Functionality:
The application serves as a task manager that allows users to manage, edit, delete, and track tasks. It also includes the option to mark tasks as completed and filter tasks based on their status. The tasks are saved in a file for persistent storage and loaded upon program startup. The main feature of the application is that the list of tasks is encrypted when stored, providing an extra layer of security. Additionally, the application includes a notes feature for storing encrypted password-related information.

Main Features:
Adding Tasks:
Users can input a task description in a text field and select a priority. Once added, the task appears in the list with additional information, such as its completion status and priority.

Editing Tasks:
Users can modify a selected task, such as changing the description or priority. Editing opens a dialog where users can enter the updated information.

Deleting Tasks:
Tasks can be removed from the list. After deletion, the list is updated, and the tasks are saved back to the file.
Marking Tasks as Completed:
Users can mark tasks as completed, which updates their status in the list. Completed tasks are tagged with "(Completed)" in the display.

Filtering Tasks:
The task list can be filtered based on various criteria: "All", "Not Completed", and "Completed". This helps users quickly find specific tasks based on their status.

Task Persistence via File Storage:
All tasks are stored in a file (tasks.txt), and the application reads and writes to this file to ensure task persistence across sessions. When the application starts, it loads tasks from the file into the list.

Encryption of Task List:
The task list is encrypted when saved to the file. This ensures that the tasks are not stored in plain text and cannot be easily accessed or viewed by unauthorized parties. The encryption is applied only when saving the tasks to the file, so the tasks remain readable while the application is running.

Secure Notes for Passwords:
The application provides a secure notes feature where users can store password-related information. These notes are encrypted before being saved, ensuring that sensitive data remains protected.

Visual Enhancements for Readability:
To improve the user interface, different visual styles are applied to the task list, such as colors for priority levels or completed tasks. Icons and custom fonts can also be used to enhance the overall user experience.

In summary:
The application provides an efficient and secure way for users to manage their tasks. By encrypting the task list when saving to the file, it ensures that sensitive task information is protected. It also offers features such as task editing, filtering, and priority management, along with a user-friendly interface to improve the overall experience. Additionally, the secure notes feature allows users to store password-related information safely.

