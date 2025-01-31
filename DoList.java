import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class DoList extends Application {

    private String fileName = "tasks.txt";

    private static final String SECRET_KEY = "m180c95ad1Rfdd98fd7aC20633b830a8"; // 126-byte key for AES
    private ObservableList<String> taskList;
    private ObservableList<String> filteredTaskList;
    private TreeView<String> treeView; // Using TreeView for grouping
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

    private void chooseDatabaseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            this.fileName = selectedFile.getAbsolutePath();
            taskList.clear(); // Clear current task list
            loadTasksFromFile(); // Loading tasks from a new file
            updateTreeView(); // Updating the task display
        } else {
            System.out.println("File not selected.");
        }
    }

    // Create a button to select a file
    private Button createChooseFileButton() {
        Button chooseFileButton = new Button("Choose Database File");
        chooseFileButton.setOnAction(e -> chooseDatabaseFile()); // Calling the file selection method
        return chooseFileButton;
    }

    // Create a tab for tasks with TreeView for task categories
    private VBox createTaskTabContent() {
        VBox taskTabContent = new VBox(15);
        taskTabContent.setAlignment(Pos.CENTER);

        // Adding a button to select a file
        taskTabContent.getChildren().add(createChooseFileButton());

        // Panel for filtering
        filterComboBox = new ComboBox<>(
                FXCollections.observableArrayList("All", "Not Completed", "Completed", "With Reminder"));
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> updateTreeView());
        filterComboBox.setMaxWidth(150);

        // Create root node for the TreeView
        TreeItem<String> rootItem = new TreeItem<>("All Tasks");
        rootItem.setExpanded(true);
        treeView = new TreeView<>(rootItem);
        treeView.setPrefHeight(300);

        // Update the tree view based on filter
        updateTreeView();

        // Task control button bar
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER);

        // Button to delete a task
        Button removeButton = new Button("Remove Task");
        removeButton.setOnAction(e -> removeTask());
        removeButton.getStyleClass().add("remove-task-button"); // Applying a style to delete

        // Button to mark a task as completed
        Button markCompleteButton = new Button("Mark as Completed");
        markCompleteButton.setOnAction(e -> markComplete());
        markCompleteButton.getStyleClass().add("mark-complete-button"); // Apply style to finish

        // Button for editing a task
        Button editTaskButton = new Button("Edit Task");
        editTaskButton.setOnAction(e -> editTask());
        editTaskButton.getStyleClass().add("edit-task-button"); // Applying a style for editing

        removeButton.setOnAction(e -> removeTask());
        markCompleteButton.setOnAction(e -> markComplete());
        editTaskButton.setOnAction(e -> editTask());

        buttonPanel.getChildren().addAll(removeButton, markCompleteButton, editTaskButton);

        taskTabContent.getChildren().addAll(filterComboBox, treeView, buttonPanel);

        return taskTabContent;
    }

    // Create a tab for adding tasks with GridPane
    private VBox createAddTaskTabContent() {
        VBox addTaskTabContent = new VBox(15);
        addTaskTabContent.setAlignment(Pos.CENTER);

        GridPane formGrid = new GridPane();
        formGrid.setVgap(10);
        formGrid.setHgap(10);

        taskInputField = new TextField();
        taskInputField.setPromptText("Enter task...");
        taskInputField.setMaxWidth(300);

        priorityComboBox = new ComboBox<>(FXCollections.observableArrayList("Low", "Medium", "High"));
        priorityComboBox.setValue("Medium");

        reminderDatePicker = new DatePicker();
        reminderDatePicker.setPromptText("Set Reminder Date");

        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Set Start Date");

        categoryComboBox = new ComboBox<>(
                FXCollections.observableArrayList("Work", "Personal", "Study", "Miscellaneous"));
        categoryComboBox.setValue("Work");

        descriptionTextArea = new TextArea();
        descriptionTextArea.setPromptText("Task Description");
        descriptionTextArea.setMaxWidth(300);
        descriptionTextArea.setMaxHeight(100);

        tagsTextField = new TextField();
        tagsTextField.setPromptText("Enter tags (comma separated)");

        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> addTask());

        // Add elements to the GridPane
        formGrid.add(new Label("Task:"), 0, 0);
        formGrid.add(taskInputField, 1, 0);

        formGrid.add(new Label("Priority:"), 0, 1);
        formGrid.add(priorityComboBox, 1, 1);

        formGrid.add(new Label("Reminder Date:"), 0, 2);
        formGrid.add(reminderDatePicker, 1, 2);

        formGrid.add(new Label("Start Date:"), 0, 3);
        formGrid.add(startDatePicker, 1, 3);

        formGrid.add(new Label("Category:"), 0, 4);
        formGrid.add(categoryComboBox, 1, 4);

        formGrid.add(new Label("Description:"), 0, 5);
        formGrid.add(descriptionTextArea, 1, 5);

        formGrid.add(new Label("Tags:"), 0, 6);
        formGrid.add(tagsTextField, 1, 6);

        addTaskTabContent.getChildren().addAll(formGrid, addButton);
        return addTaskTabContent;
    }

    private void updateTreeView() {
        TreeItem<String> rootItem = new TreeItem<>("All Tasks");
        rootItem.setExpanded(true);

        String filter = filterComboBox.getValue();

        // Map to store tasks grouped by category and month
        Map<String, TreeItem<String>> categoryMap = new HashMap<>();

        for (String task : taskList) {
            // Apply filter logic
            if (filter.equals("Not Completed") && task.contains("(Not Completed)") ||
                    filter.equals("Completed") && task.contains("(Completed)") ||
                    filter.equals("With Reminder") && task.contains("(Reminder)") ||
                    filter.equals("All")) {

                // Extract start date and category
                String startDate = extractStartDate(task);
                String category = extractCategory(task);

                // If the start date exists, group by month
                if (startDate != null) {
                    String month = getMonthFromDate(startDate); // Get the month from the start date

                    // Create or retrieve the category node
                    TreeItem<String> categoryNode = categoryMap.computeIfAbsent(category, k -> {
                        TreeItem<String> newCategoryNode = new TreeItem<>(k);
                        rootItem.getChildren().add(newCategoryNode);
                        return newCategoryNode;
                    });

                    // Create or retrieve the month node under the category
                    TreeItem<String> monthNode = categoryNode.getChildren().stream()
                            .filter(child -> child.getValue().equals(month))
                            .findFirst()
                            .orElseGet(() -> {
                                TreeItem<String> newMonthNode = new TreeItem<>(month);
                                categoryNode.getChildren().add(newMonthNode);
                                return newMonthNode;
                            });

                    // Add the task to the respective month node
                    monthNode.getChildren().add(new TreeItem<>(task));
                }
            }
        }

        treeView.setRoot(rootItem);
    }

    // Extract the start date from the task string (in the format [Start Date:
    // yyyy-MM-dd])
    private String extractStartDate(String task) {
        int start = task.indexOf("[Start Date: ");
        int end = task.indexOf("]", start);
        if (start != -1 && end != -1) {
            return task.substring(start + 13, end); // Extract the date part
        }
        return null;
    }

    // Convert date string to month (e.g., "2025-01-01" -> "January 2025")
    private String getMonthFromDate(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date); // Parse date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy"); // Format to month and year
            return localDate.format(formatter);
        } catch (DateTimeParseException e) {
            return "Invalid Date";
        }
    }

    // error handling
    private String extractCategory(String task) {
        int start = task.indexOf("[Category: ");
        int end = task.indexOf("]", start);
        if (start != -1 && end != -1) {
            return task.substring(start + 10, end);
        }
        return "Uncategorized";
    }

    // Adding a task
    private void addTask() {
        String task = taskInputField.getText();
        String description = descriptionTextArea.getText();
        String tags = tagsTextField.getText();
        String priority = priorityComboBox.getValue();
        String category = categoryComboBox.getValue();
        String startDate = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "No Start Date";
        String reminderDate = reminderDatePicker.getValue() != null ? reminderDatePicker.getValue().toString()
                : "No Reminder";

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

            taskList.add(taskText); // Add task to the list
            updateTreeView(); // Update tree view with new task
            taskInputField.clear();
            descriptionTextArea.clear();
            tagsTextField.clear();
            reminderDatePicker.setValue(null);
            startDatePicker.setValue(null);
            priorityComboBox.setValue("Medium");
            categoryComboBox.setValue("Work");
            saveTasksToFile(); // Encrypt only when saving to file
        }
    }

    // Saving tasks to a file with encryption
    private void saveTasksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String task : taskList) {
                writer.write(encrypt(task)); // Encrypt the task when saving
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Decrypt the task text
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

    private void loadTasksFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                taskList.add(decrypt(line)); // Decoding the string
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
        } catch (IOException e) {
            System.out.println("Error reading file: " + fileName);
            e.printStackTrace();
        }
    }

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

    // removeTask
    private void removeTask() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getParent() != null) {
            String selectedTask = selectedItem.getValue();
            taskList.remove(selectedTask);
            updateTreeView(); // Refresh tree view
            saveTasksToFile();
        }
    }

    // Mark the task as completed
    private void markComplete() {
        String selectedTask = treeView.getSelectionModel().getSelectedItem().getValue();
        if (selectedTask != null && !selectedTask.contains("(Completed)")) {
            int index = taskList.indexOf(selectedTask);
            taskList.set(index, selectedTask.replace("(Not Completed)", "(Completed)"));
            updateTreeView(); // Refresh tree view
            saveTasksToFile();
        }
    }

    // Editing a task
    private void editTask() {
        String selectedTask = treeView.getSelectionModel().getSelectedItem().getValue();
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
                updateTreeView();

                // Save the updated task list to a file (encrypt tasks)
                saveTasksToFile();
            });
        }
    }
}
