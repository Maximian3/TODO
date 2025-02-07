import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
    private ListView<String> passwordListView;

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
        Tab passwordTab = new Tab("Passwords");
        passwordTab.setClosable(false);
        passwordTab.setContent(createPasswordTabContent());
        // Tab for tasks
        Tab tasksTab = new Tab("Tasks");
        tasksTab.setClosable(false);
        tasksTab.setContent(createTaskTabContent());
        // Tab for adding tasks
        Tab addTaskTab = new Tab("Add Task");
        addTaskTab.setClosable(false);
        addTaskTab.setContent(createAddTaskTabContent());

        tabPane.getTabs().addAll(tasksTab, addTaskTab, passwordTab);

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

    private VBox createPasswordTabContent() {
        VBox passwordTabContent = new VBox(15);
        passwordTabContent.setAlignment(Pos.CENTER);
    
        // Input fields for password details
        TextField websiteField = new TextField();
        websiteField.setPromptText("Enter Website");
    
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter Username");
    
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
    
        Button savePasswordButton = new Button("Save Password");
        savePasswordButton.setOnAction(e -> savePassword(websiteField.getText(), usernameField.getText(), passwordField.getText()));
    
        // ListView for displaying passwords
        passwordListView = new ListView<>();
        ObservableList<String> passwordList = FXCollections.observableArrayList();
        passwordListView.setItems(passwordList);
    
        // Load saved passwords from file
        loadPasswordsFromFile(passwordList);  // Load passwords from the file
    
        // Button to edit a password
        Button editPasswordButton = new Button("Edit Password");
        editPasswordButton.setOnAction(e -> editPassword());
    
        // Button to remove a password
        Button removePasswordButton = new Button("Remove Password");
        removePasswordButton.setOnAction(e -> removePassword());
    
        // Adding elements to the password tab content
        passwordTabContent.getChildren().addAll(
                websiteField, usernameField, passwordField, savePasswordButton, 
                passwordListView, editPasswordButton, removePasswordButton
        );
    
        return passwordTabContent;
    }
    
    
    private void editPassword() {
    String selectedPassword = passwordListView.getSelectionModel().getSelectedItem();
    if (selectedPassword != null) {
        
        String[] parts = selectedPassword.split(" \\| ");
        if (parts.length == 3) {
            String website = parts[0].replace("Website: ", "").trim();
            String username = parts[1].replace("Username: ", "").trim();
            String password = parts[2].replace("Password: ", "").trim();

            // 
            Dialog<List<String>> dialog = new Dialog<>();
            dialog.setTitle("Edit Entry");
            dialog.setHeaderText("Edit your password entry");

            //  OK и Cancel
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Imput
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField websiteField = new TextField(website);
            TextField usernameField = new TextField(username);
            PasswordField passwordField = new PasswordField();
            passwordField.setText(password);

            grid.add(new Label("Website:"), 0, 0);
            grid.add(websiteField, 1, 0);
            grid.add(new Label("Username:"), 0, 1);
            grid.add(usernameField, 1, 1);
            grid.add(new Label("Password:"), 0, 2);
            grid.add(passwordField, 1, 2);

            dialog.getDialogPane().setContent(grid);

            // Convert
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return Arrays.asList(websiteField.getText(), usernameField.getText(), passwordField.getText());
                }
                return null;
            });

            Optional<List<String>> result = dialog.showAndWait();

            result.ifPresent(updatedValues -> {
                String updatedWebsite = updatedValues.get(0);
                String updatedUsername = updatedValues.get(1);
                String updatedPassword = updatedValues.get(2);

                // Encrypt
                String encryptedWebsite = encrypt(updatedWebsite);
                String encryptedUsername = encrypt(updatedUsername);
                String encryptedPassword = encrypt(updatedPassword);

                // New string
                String encryptedRecord = "Website: " + encryptedWebsite + " | Username: " + encryptedUsername + " | Password: " + encryptedPassword;

                // update`ListView`
                int index = passwordListView.getItems().indexOf(selectedPassword);
                if (index != -1) {
                    passwordListView.getItems().set(index, decryptFieldsInRecord(encryptedRecord)); // show 
                }

                // update file
                updatePasswordInFile(selectedPassword, encryptedRecord);
            });
        }
    }
}

    
    private void updatePasswordInFile(String oldRecord, String newEncryptedRecord) {
        List<String> lines = new ArrayList<>();
        
        // reed all strings
        try (BufferedReader reader = new BufferedReader(new FileReader("passwords.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 
                if (line.equals(oldRecord)) {
                    lines.add(newEncryptedRecord); // 
                } else {
                    lines.add(line); // 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        // rewrite
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("passwords.txt"))) {
            for (String updatedLine : lines) {
                writer.write(updatedLine);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    
    private void removePassword() {
        String selectedPassword = passwordListView.getSelectionModel().getSelectedItem();
        if (selectedPassword != null) {
            // Remove the selected password from the ListView
            passwordListView.getItems().remove(selectedPassword);
    
            // Save the updated password list to the file
            savePasswordsToFile();
        }
    }

    
    private void savePassword(String website, String username, String password) {
        if (!website.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            // 
            String passwordRecord = "Website: " + website + " | Username: " + username + " | Password: " + password;
    
            // add  ListView
            passwordListView.getItems().add(passwordRecord);
    
            // savePasswords
            savePasswordsToFile();
        }
    }
    
    
    private void savePasswordsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("passwords.txt"))) {
            for (String passwordRecord : passwordListView.getItems()) {
                // 
                String encryptedPasswordRecord = encryptFieldsInRecord(passwordRecord);
                writer.write(encryptedPasswordRecord);  // save pass
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    
    private String encryptFieldsInRecord(String passwordRecord) {
        // sep 
        String[] parts = passwordRecord.split(" \\| ");
        if (parts.length == 3) {
            // exe
            String website = parts[0].replace("Website: ", "").trim();
            String username = parts[1].replace("Username: ", "").trim();
            String password = parts[2].replace("Password: ", "").trim();
            
            // encore
            String encryptedWebsite = encrypt(website);
            String encryptedUsername = encrypt(username);
            String encryptedPassword = encrypt(password);
            
            // return
            return "Website: " + encryptedWebsite + " | Username: " + encryptedUsername + " | Password: " + encryptedPassword;
        }
        return passwordRecord;  // ret 
    }
    
    

    private String decryptFieldsInRecord(String encryptedRecord) {
        // spring
        String[] parts = encryptedRecord.split(" \\| ");
        if (parts.length == 3) {
            // exe
            String encryptedWebsite = parts[0].replace("Website: ", "").trim();
            String encryptedUsername = parts[1].replace("Username: ", "").trim();
            String encryptedPassword = parts[2].replace("Password: ", "").trim();
    
            // decrypt
            String website = decrypt(encryptedWebsite);
            String username = decrypt(encryptedUsername);
            String password = decrypt(encryptedPassword);
    
            // return
            return "Website: " + website + " | Username: " + username + " | Password: " + password;
        }
        return encryptedRecord;  // ret
    }
    
    private String decrypt(String encryptedData) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    
    private void createPasswordListView() {
        passwordListView = new ListView<>();
        ObservableList<String> passwordList = FXCollections.observableArrayList();
    
        // add pass from file
        loadPasswordsFromFile(passwordList);
        passwordListView.setItems(passwordList);
    
        // new menu
        ContextMenu contextMenu = new ContextMenu();
        
        // copy log
        MenuItem copyLoginItem = new MenuItem("Copy Login");
        copyLoginItem.setOnAction(event -> {
            String selectedPassword = passwordListView.getSelectionModel().getSelectedItem();
            if (selectedPassword != null) {
                String login = extractLogin(selectedPassword);  // exe log
                copyToClipboard(login);  // copy to Clipboard
            }
        });
    
        // copy pass
        MenuItem copyPasswordItem = new MenuItem("Copy Password");
        copyPasswordItem.setOnAction(event -> {
            String selectedPassword = passwordListView.getSelectionModel().getSelectedItem();
            if (selectedPassword != null) {
                String encryptedPassword = extractPassword(selectedPassword);  // exe pass
                String decryptedPassword = decrypt(encryptedPassword);  // encryption 
                copyToClipboard(decryptedPassword);  // copy to Clipboard
            }
        });
    
        contextMenu.getItems().addAll(copyLoginItem, copyPasswordItem);
        
        // install menu on ListView
        passwordListView.setContextMenu(contextMenu);
    }
    
    // exe log
    private String extractLogin(String passwordRecord) {
        int start = passwordRecord.indexOf("Username: ");
        int end = passwordRecord.indexOf(" | Password: ", start);
        if (start != -1 && end != -1) {
            return passwordRecord.substring(start + 10, end);  // return 
        }
        return "";
    }
    
    // exe pass
    private String extractPassword(String passwordRecord) {
        int start = passwordRecord.indexOf("Password: ");
        if (start != -1) {
            return passwordRecord.substring(start + 10);  // return pass
        }
        return "";
    }
    
    // copy to Clipboard
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
    
    
    private void loadPasswordsFromFile(ObservableList<String> passwordList) {
        passwordListView.getItems().clear();  // reset buffer
        try (BufferedReader reader = new BufferedReader(new FileReader("passwords.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // check line
                if (!line.trim().isEmpty()) {
                    // encr
                    String decryptedRecord = decryptFieldsInRecord(line);
                    
                    // check
                    if (isValidRecord(decryptedRecord)) {
                        // add in ListView
                        passwordList.add(decryptedRecord);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // valid f
    private boolean isValidRecord(String record) {
        // check "Website", "Username" и "Password" with not null 
        return record.contains("Website: ") && record.contains("Username: ") && record.contains("Password: ");
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
            return Base64.getEncoder().encodeToString(encryptedData);  // Вreturn string in Base64
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

    private void editTask() {
        String selectedTask = treeView.getSelectionModel().getSelectedItem().getValue();
        if (selectedTask != null) {
            // new dialog with Alert 
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Edit Task");
            alert.setHeaderText("Edit your task");
    
            // new panel input
            TextField taskField = new TextField(selectedTask);
            taskField.setPrefWidth(700);  // install width
            taskField.setPrefHeight(30);  // install height
    
            // add text in sector 
            VBox vbox = new VBox();
            vbox.getChildren().add(taskField);
            alert.getDialogPane().setContent(vbox);
    
            // add button
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(saveButtonType, ButtonType.CANCEL);
    
            // Display the dialog and process the entered text
            alert.showAndWait().ifPresent(result -> {
                if (result == saveButtonType) {
                    String newTask = taskField.getText();
    
                    // Get the index of the task in the list
                    int index = taskList.indexOf(selectedTask);
    
                    // Check if the task is completed or with a reminder
                    boolean isCompleted = selectedTask.contains("(Completed)");
                    boolean isReminder = selectedTask.contains("(Reminder)");
    
                    // Create an updated task line
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
    
                    // Applying filtering after editing
                    updateTreeView();
    
                    // Save the updated task list to a file (with encryption)
                    saveTasksToFile();
                }
            });
        }
    }
}    
    
