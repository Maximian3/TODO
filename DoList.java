import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.*;

public class DoList extends Application {

    private static final String FILE_NAME = "tasks.txt";
    private static final String SECRET_KEY = "m180c95ad1Rfdd98fd7aC20633b830a8";  // 126-byte key for AES
    private ObservableList<String> taskList;
    private ObservableList<String> filteredTaskList;
    private ListView<String> listView;
    private TextField taskInputField;
    private DatePicker reminderDatePicker;
    private DatePicker startDatePicker;
    private ComboBox<String> priorityComboBox;
    private ComboBox<String> categoryComboBox;
    private TextArea descriptionTextArea;
    private TextField tagsTextField;
    private ComboBox<String> filterComboBox;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Loading tasks from file
        taskList = FXCollections.observableArrayList();
        filteredTaskList = FXCollections.observableArrayList();
        loadTasksFromFile();

        // Setting up the main window
        primaryStage.setTitle("Task Manager");
        primaryStage.setWidth(700);
        primaryStage.setHeight(550);

        // Create a panel for tabs
        TabPane tabPane = new TabPane();

        // Tab for tasks
        Tab tasksTab = new Tab("Tasks");
        tasksTab.setClosable(false);
        tasksTab.setContent(createTaskTabContent());

        // Tab for adding tasks
        Tab addTaskTab = new Tab("Add Task");
        addTaskTab.setClosable(false);
        addTaskTab.setContent(createAddTaskTabContent());

        tabPane.getTabs().addAll(tasksTab, addTaskTab);

        // Setting up the scene and displaying the window
        Scene scene = new Scene(tabPane, 700, 550);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Create a tab for tasks
    private VBox createTaskTabContent() {
        VBox taskTabContent = new VBox(15);
        taskTabContent.setAlignment(Pos.CENTER);

       // Panel for filtering
        filterComboBox = new ComboBox<>(FXCollections.observableArrayList("All", "Not Completed", "Completed", "With Reminder"));
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> filterTasks());
        filterComboBox.setMaxWidth(150);

        // Task list
        listView = new ListView<>(filteredTaskList);
        listView.setPrefHeight(250);

        // Task control button bar
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER);

        Button removeButton = new Button("Remove Task");
        Button markCompleteButton = new Button("Mark as Completed");
        Button editTaskButton = new Button("Edit Task");
        

        removeButton.setOnAction(e -> removeTask());
        markCompleteButton.setOnAction(e -> markComplete());
        editTaskButton.setOnAction(e -> editTask());
        

        buttonPanel.getChildren().addAll(removeButton, markCompleteButton, editTaskButton);

        taskTabContent.getChildren().addAll(filterComboBox, listView, buttonPanel);
        filterTasks(); // Task control button bar

        return taskTabContent;
    }

    // Create a tab for adding tasks
    private VBox createAddTaskTabContent() {
        VBox addTaskTabContent = new VBox(15);
        addTaskTabContent.setAlignment(Pos.CENTER);

        taskInputField = new TextField();
        taskInputField.setPromptText("Enter task...");
        taskInputField.setMaxWidth(300);

        // Combo for choosing priority
        priorityComboBox = new ComboBox<>(FXCollections.observableArrayList("Low", "Medium", "High"));
        priorityComboBox.setValue("Medium");

        // Date for reminders
        reminderDatePicker = new DatePicker();
        reminderDatePicker.setPromptText("Set Reminder Date");

        // Task start date
        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Set Start Date");

        // Task category
        categoryComboBox = new ComboBox<>(FXCollections.observableArrayList("Work", "Personal", "Study", "Miscellaneous"));
        categoryComboBox.setValue("Work");

        // Task description
        descriptionTextArea = new TextArea();
        descriptionTextArea.setPromptText("Task Description");
        descriptionTextArea.setMaxWidth(300);
        descriptionTextArea.setMaxHeight(100);

        //Task tags
        tagsTextField = new TextField();
        tagsTextField.setPromptText("Enter tags (comma separated)");

        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> addTask());

        addTaskTabContent.getChildren().addAll(
            taskInputField, 
            priorityComboBox, 
            reminderDatePicker, 
            startDatePicker,
            categoryComboBox,
            descriptionTextArea,
            tagsTextField,
            addButton
        );

        return addTaskTabContent;
    }

    // Filtering tasks
    private void filterTasks() {
        String filter = filterComboBox.getValue();
        filteredTaskList.clear();

        for (String task : taskList) {
            if ("All".equals(filter)) {
                filteredTaskList.add(task);
            } else if ("Not Completed".equals(filter) && task.contains("(Not Completed)")) {
                filteredTaskList.add(task);
            } else if ("Completed".equals(filter) && task.contains("(Completed)")) {
                filteredTaskList.add(task);
            } else if ("With Reminder".equals(filter) && task.contains("(Reminder)")) {
                filteredTaskList.add(task);
            }
        }
    }

    // Adding a task
    private void addTask() {
        String task = taskInputField.getText();
        String description = descriptionTextArea.getText();
        String tags = tagsTextField.getText();
        String priority = priorityComboBox.getValue();
        String category = categoryComboBox.getValue();
        String startDate = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "No Start Date";
        String reminderDate = reminderDatePicker.getValue() != null ? reminderDatePicker.getValue().toString() : "No Reminder";

        if (!task.isEmpty()) {
            String taskText = task + " (Not Completed)";

            if (!description.isEmpty()) {
                taskText += " [Description: " + description + "]";
            }

            if (!tags.isEmpty()) {
                taskText += " [Tags: " + tags + "]";
            }

            taskText += " [Priority: " + priority + "]";
            taskText += " [Category: " + category + "]";
            taskText += " [Start Date: " + startDate + "]";
            taskText += " (Reminder for " + reminderDate + ")";

            taskList.add(taskText);  // Leave the task in a readable form in the program
            filterTasks(); // Refresh filtered tasks
            taskInputField.clear();
            descriptionTextArea.clear();
            tagsTextField.clear();
            reminderDatePicker.setValue(null);
            startDatePicker.setValue(null);
            priorityComboBox.setValue("Medium");
            categoryComboBox.setValue("Work");
            saveTasksToFile();  // Encrypt only when saving to file
        }
    }

    // Encrypt the task text
    private String encrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Decipher the task text
    private String decrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedData = Base64.getDecoder().decode(data);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

   // Loading tasks from a file with decryption
    private void loadTasksFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                taskList.add(decrypt(line));  // Decrypt the task on boot
            }
        } catch (IOException e) {
            System.out.println("No previous tasks found. Starting fresh.");
        }
    }

    // Saving tasks to a file with encryption
    private void saveTasksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (String task : taskList) {
                writer.write(encrypt(task));  // Encrypt the task when saving
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Delete task
    private void removeTask() {
        String selectedTask = listView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            taskList.remove(selectedTask);
            filterTasks(); // Refresh filtered tasks
            saveTasksToFile();
        }
    }

    // Mark the task as completed
    private void markComplete() {
        String selectedTask = listView.getSelectionModel().getSelectedItem();
        if (selectedTask != null && !selectedTask.contains("(Completed)")) {
            int index = taskList.indexOf(selectedTask);
            taskList.set(index, selectedTask.replace("(Not Completed)", "(Completed)"));
            filterTasks(); // Refresh filtered tasks
            saveTasksToFile();
        }
    }

    // Editing a task
    private void editTask() {
        String selectedTask = listView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            // Open a dialog box to edit the task
            TextInputDialog dialog = new TextInputDialog(selectedTask);
            dialog.setTitle("Edit Task");
            dialog.setHeaderText("Edit your task");
            dialog.setContentText("Task:");

            dialog.showAndWait().ifPresent(newTask -> {
                // Get the index of the task in the list
                int index = taskList.indexOf(selectedTask);

                // Check if the task contains a status (e.g. "(Completed)")
                boolean isCompleted = selectedTask.contains("(Completed)");
                boolean isReminder = selectedTask.contains("(Reminder)");

                // Compose an updated task string
                String updatedTask = newTask;
                if (isReminder) {
                    updatedTask += " (Reminder)";
                } else if (isCompleted) {
                    updatedTask += " (Completed)";
                } else {
                    updatedTask += " (Not Completed)";
                }

                // Update the task in the list
                taskList.set(index, updatedTask);

                // Apply task filtering after editing
                filterTasks();
                
                // Save the updated task list to a file (encrypt tasks)
                saveTasksToFile();
            });
        }
    }



}
