import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class VotingSystem {
    private static final String DB_URL = "jdbc:sqlite:voting_system.db";
    private static Connection conn;
    private static String currentUser = null;
    private static boolean isAdmin = false;
    private static final Color PRIMARY_COLOR = new Color(0, 35, 102); // Dark blue
    private static final Color SECONDARY_COLOR = new Color(255, 215, 0); // Gold
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final String IMAGE_DIR = "candidate_images/";
    private static JFileChooser fileChooser;

    public static void main(String[] args) {
        // Create image directory if it doesn't exist
        new File(IMAGE_DIR).mkdirs();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeDatabase();
        createLoginWindow();
    }

    private static void initializeDatabase() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();

            // Create tables
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "student_id TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "has_voted BOOLEAN DEFAULT FALSE," +
                    "is_admin BOOLEAN DEFAULT FALSE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS candidates (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "position TEXT NOT NULL," +
                    "role TEXT," +
                    "image_path TEXT," +
                    "votes INTEGER DEFAULT 0)");

            // Check if any admin exists
            ResultSet adminCheck = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE is_admin = TRUE");
            if (adminCheck.getInt(1) == 0) {
                // Create default admin account if none exists
                String defaultAdminID = "admin";
                String defaultAdminPass = "admin123";
                String defaultAdminName = "System Administrator";

                try {
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO users (student_id, password, name, is_admin) VALUES (?, ?, ?, ?)");
                    pstmt.setString(1, defaultAdminID);
                    pstmt.setString(2, defaultAdminPass);
                    pstmt.setString(3, defaultAdminName);
                    pstmt.setBoolean(4, true);
                    pstmt.executeUpdate();
                    System.out.println("Created default admin account");
                } catch (SQLException e) {
                    System.out.println("Default admin account already exists or couldn't be created");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog(null, "Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void createLoginWindow() {
        JFrame frame = new JFrame("RTU Electronic Voting System - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 450);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        headerPanel.setPreferredSize(new Dimension(frame.getWidth(), 80));

        JLabel titleLabel = new JLabel("RTU ELECTRONIC VOTING SYSTEM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Gold accent bar under header
        JPanel accentBar = new JPanel();
        accentBar.setBackground(SECONDARY_COLOR);
        accentBar.setPreferredSize(new Dimension(frame.getWidth(), 4));
        headerPanel.add(accentBar, BorderLayout.SOUTH);

        frame.add(headerPanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Login title
        JLabel loginLabel = new JLabel("Login to Your Account", JLabel.CENTER);
        loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        loginLabel.setForeground(PRIMARY_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(loginLabel, gbc);

        // Student ID Field
        JLabel studentIdLabel = new JLabel("Student ID:");
        studentIdLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        mainPanel.add(studentIdLabel, gbc);

        JTextField studentIdField = new JTextField();
        studentIdField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 1;
        mainPanel.add(studentIdField, gbc);

        // Password Field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(passwordLabel, gbc);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 2;
        mainPanel.add(passwordField, gbc);

        // Login Button
        JButton loginButton = createStyledButton("LOGIN", SECONDARY_COLOR, PRIMARY_COLOR);
        loginButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)
        ));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 0, 5, 0);
        mainPanel.add(loginButton, gbc);

        // Register Button
        JButton registerButton = createStyledButton("REGISTER", Color.WHITE, PRIMARY_COLOR);
        registerButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 0, 5, 0);
        mainPanel.add(registerButton, gbc);

        // Action listeners
        loginButton.addActionListener(e -> {
            String studentId = studentIdField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (studentId.isEmpty() || password.isEmpty()) {
                showErrorDialog(frame, "Please enter both student ID and password");
                return;
            }

            try {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT name, has_voted, is_admin FROM users WHERE student_id = ? AND password = ?");
                stmt.setString(1, studentId);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    currentUser = rs.getString("name");
                    isAdmin = rs.getBoolean("is_admin");

                    if (isAdmin) {
                        frame.dispose();
                        createAdminDashboard();
                    } else if (rs.getBoolean("has_voted")) {
                        showErrorDialog(frame, "You have already voted. Each student can only vote once.");
                    } else {
                        frame.dispose();
                        createVotingWindow();
                    }
                } else {
                    showErrorDialog(frame, "Invalid student ID or password");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrorDialog(frame, "Database error: " + ex.getMessage());
            }
        });

        registerButton.addActionListener(e -> {
            showStudentRegistrationDialog(frame);
        });

        // Footer Panel with gold color
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(SECONDARY_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        footerPanel.setPreferredSize(new Dimension(frame.getWidth(), 40));

        JLabel footerLabel = new JLabel("© 2025 RTU Voting System");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerLabel.setForeground(PRIMARY_COLOR);
        footerPanel.add(footerLabel);

        frame.add(footerPanel, BorderLayout.SOUTH);
        frame.add(mainPanel, BorderLayout.CENTER);

        // Center and show
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showStudentRegistrationDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Student Registration", true);
        dialog.setSize(450, 400);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        JLabel titleLabel = new JLabel("STUDENT REGISTRATION");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Student ID Field
        JLabel studentIdLabel = new JLabel("Student ID:");
        studentIdLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(studentIdLabel, gbc);

        JTextField studentIdField = new JTextField();
        studentIdField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(studentIdField, gbc);

        // Name Field
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(nameLabel, gbc);

        JTextField nameField = new JTextField();
        nameField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(nameField, gbc);

        // Password Field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(passwordLabel, gbc);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 2;
        inputPanel.add(passwordField, gbc);

        // Confirm Password Field
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(confirmPasswordLabel, gbc);

        JPasswordField confirmPasswordField = new JPasswordField();
        confirmPasswordField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 3;
        inputPanel.add(confirmPasswordField, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        JButton registerButton = createStyledButton("Register", SECONDARY_COLOR, PRIMARY_COLOR);
        JButton cancelButton = createStyledButton("Cancel", Color.WHITE, PRIMARY_COLOR);

        registerButton.addActionListener(e -> {
            String studentId = studentIdField.getText().trim();
            String name = nameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (studentId.isEmpty() || name.isEmpty() || password.isEmpty()) {
                showErrorDialog(dialog, "Please fill all fields");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showErrorDialog(dialog, "Passwords do not match");
                return;
            }

            try {
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO users (student_id, name, password) VALUES (?, ?, ?)");
                pstmt.setString(1, studentId);
                pstmt.setString(2, name);
                pstmt.setString(3, password);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(dialog,
                        "Registration successful! You can now login with your credentials.",
                        "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (SQLException ex) {
                if (ex.getMessage().contains("UNIQUE constraint failed")) {
                    showErrorDialog(dialog, "This student ID is already registered");
                } else {
                    showErrorDialog(dialog, "Error during registration: " + ex.getMessage());
                }
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(registerButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static void createVotingWindow() {
        JFrame frame = new JFrame("RTU Voting System - Cast Your Vote");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 800); // Slightly larger for better layout
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.WHITE);

        // Header Panel with improved styling
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JLabel titleLabel = new JLabel("CAST YOUR VOTE", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        welcomeLabel.setForeground(new Color(220, 220, 220));
        headerPanel.add(welcomeLabel, BorderLayout.EAST);

        frame.add(headerPanel, BorderLayout.NORTH);

        // Main content panel with tabbed interface
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tabbedPane.setBackground(new Color(245, 245, 245));
        tabbedPane.setForeground(PRIMARY_COLOR);

        // Candidate details panel with professional styling
        JPanel candidateDetailPanel = new JPanel(new BorderLayout());
        candidateDetailPanel.setBackground(new Color(250, 250, 250));
        candidateDetailPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Photo panel with shadow effect
        JPanel photoPanel = new JPanel(new BorderLayout());
        photoPanel.setBackground(Color.WHITE);
        photoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        photoPanel.setPreferredSize(new Dimension(280, 280));

        JLabel candidateImageLabel = new JLabel("", JLabel.CENTER);
        candidateImageLabel.setVerticalAlignment(JLabel.CENTER);
        candidateImageLabel.setHorizontalAlignment(JLabel.CENTER);

        // Default placeholder image
        try {
            ImageIcon placeholder = new ImageIcon(ImageIO.read(
                    VotingSystem.class.getResourceAsStream("/placeholder.png")));
            Image scaled = placeholder.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            candidateImageLabel.setIcon(new ImageIcon(scaled));
            candidateImageLabel.setText("");
        } catch (Exception e) {
            candidateImageLabel.setIcon(null);
            candidateImageLabel.setText("No Image Available");
            candidateImageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            candidateImageLabel.setForeground(new Color(150, 150, 150));
        }

        photoPanel.add(candidateImageLabel, BorderLayout.CENTER);
        candidateDetailPanel.add(photoPanel, BorderLayout.NORTH);

        // Candidate details area with improved styling
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JLabel detailsTitle = new JLabel("CANDIDATE DETAILS");
        detailsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        detailsTitle.setForeground(PRIMARY_COLOR);
        detailsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        detailsPanel.add(detailsTitle, BorderLayout.NORTH);

        JTextArea candidateDetailsArea = new JTextArea();
        candidateDetailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        candidateDetailsArea.setEditable(false);
        candidateDetailsArea.setLineWrap(true);
        candidateDetailsArea.setWrapStyleWord(true);
        candidateDetailsArea.setBackground(new Color(250, 250, 250));
        candidateDetailsArea.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        candidateDetailsArea.setText("Select a candidate to view their details");

        JScrollPane detailsScrollPane = new JScrollPane(candidateDetailsArea);
        detailsScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        detailsPanel.add(detailsScrollPane, BorderLayout.CENTER);

        candidateDetailPanel.add(detailsPanel, BorderLayout.CENTER);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT position FROM candidates");

            Map<String, ButtonGroup> positionGroups = new HashMap<>();
            Map<String, JPanel> positionPanels = new HashMap<>();

            while (rs.next()) {
                String position = rs.getString("position");
                ButtonGroup group = new ButtonGroup();
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBackground(Color.WHITE);
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                JLabel positionLabel = new JLabel(position.toUpperCase());
                positionLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                positionLabel.setForeground(PRIMARY_COLOR);
                positionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
                panel.add(positionLabel);
                panel.add(Box.createRigidArea(new Dimension(0, 10)));

                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT id, name, role, image_path FROM candidates WHERE position = ?");
                pstmt.setString(1, position);
                ResultSet candidates = pstmt.executeQuery();

                while (candidates.next()) {
                    int candidateId = candidates.getInt("id");
                    String candidateName = candidates.getString("name");
                    String candidateRole = candidates.getString("role");
                    String imagePath = candidates.getString("image_path");

                    JPanel candidatePanel = new JPanel(new BorderLayout(15, 0));
                    candidatePanel.setBackground(Color.WHITE);
                    candidatePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                    JRadioButton radioButton = new JRadioButton(candidateName);
                    radioButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    radioButton.setBackground(Color.WHITE);
                    radioButton.setActionCommand(String.valueOf(candidateId));
                    group.add(radioButton);

                    radioButton.addActionListener(e -> {
                        if (radioButton.isSelected()) {
                            StringBuilder details = new StringBuilder();
                            details.append("Name: ").append(candidateName).append("\n\n");
                            details.append("Position: ").append(position).append("\n\n");
                            if (candidateRole != null && !candidateRole.isEmpty()) {
                                details.append("Role/Duties:\n").append(candidateRole);
                            } else {
                                details.append("Role/Duties: Not specified");
                            }
                            candidateDetailsArea.setText(details.toString());

                            // Load and display candidate image professionally
                            try {
                                if (imagePath != null && !imagePath.isEmpty()) {
                                    ImageIcon icon = new ImageIcon(imagePath);
                                    Image img = icon.getImage();
                                    // Scale image maintaining aspect ratio
                                    int width = 250;
                                    int height = 250;
                                    if (img.getWidth(null) > img.getHeight(null)) {
                                        height = (int) (250 * ((double) img.getHeight(null) / img.getWidth(null)));
                                    } else {
                                        width = (int) (250 * ((double) img.getWidth(null) / img.getHeight(null)));
                                    }
                                    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                                    candidateImageLabel.setIcon(new ImageIcon(scaled));
                                    candidateImageLabel.setText("");
                                } else {
                                    throw new Exception("No image path");
                                }
                            } catch (Exception ex) {
                                // Use placeholder if image loading fails
                                try {
                                    ImageIcon placeholder = new ImageIcon(ImageIO.read(
                                            VotingSystem.class.getResourceAsStream("/placeholder.png")));
                                    Image scaled = placeholder.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                                    candidateImageLabel.setIcon(new ImageIcon(scaled));
                                    candidateImageLabel.setText("");
                                } catch (Exception e1) {
                                    candidateImageLabel.setIcon(null);
                                    candidateImageLabel.setText("No Image Available");
                                    candidateImageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                                    candidateImageLabel.setForeground(new Color(150, 150, 150));
                                }
                            }
                        }
                    });

                    candidatePanel.add(radioButton, BorderLayout.CENTER);
                    panel.add(candidatePanel);
                    panel.add(Box.createRigidArea(new Dimension(0, 10)));
                }

                positionGroups.put(position, group);
                positionPanels.put(position, panel);

                JScrollPane scrollPane = new JScrollPane(panel);
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                tabbedPane.addTab(position, scrollPane);
            }

            // Create split pane with improved proportions
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, candidateDetailPanel);
            splitPane.setDividerLocation(700);
            splitPane.setResizeWeight(0.7);
            splitPane.setBorder(BorderFactory.createEmptyBorder());
            frame.add(splitPane, BorderLayout.CENTER);

            // Footer panel with action buttons
            JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
            footerPanel.setBackground(SECONDARY_COLOR);
            footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

            JButton submitButton = createStyledButton("SUBMIT VOTES", SECONDARY_COLOR, PRIMARY_COLOR);
            submitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            submitButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                    BorderFactory.createEmptyBorder(10, 30, 10, 30)
            ));

            JButton logoutButton = createStyledButton("LOGOUT", SECONDARY_COLOR, PRIMARY_COLOR);
            logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            logoutButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                    BorderFactory.createEmptyBorder(10, 30, 10, 30)
            ));

            submitButton.addActionListener(e -> {
                Map<String, Integer> votes = new HashMap<>();
                boolean allPositionsVoted = true;

                for (Map.Entry<String, ButtonGroup> entry : positionGroups.entrySet()) {
                    String position = entry.getKey();
                    ButtonGroup group = entry.getValue();

                    if (group.getSelection() == null) {
                        allPositionsVoted = false;
                        tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(position));
                        showErrorDialog(frame, "Please select a candidate for " + position);
                        break;
                    } else {
                        votes.put(position, Integer.parseInt(group.getSelection().getActionCommand()));
                    }
                }

                if (allPositionsVoted) {
                    int confirm = JOptionPane.showConfirmDialog(frame,
                            "Are you sure you want to submit your votes? You cannot change them afterward.",
                            "Confirm Submission", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            conn.setAutoCommit(false);

                            for (Map.Entry<String, Integer> entry : votes.entrySet()) {
                                PreparedStatement pstmt = conn.prepareStatement(
                                        "UPDATE candidates SET votes = votes + 1 WHERE id = ?");
                                pstmt.setInt(1, entry.getValue());
                                pstmt.executeUpdate();
                            }

                            PreparedStatement pstmt = conn.prepareStatement(
                                    "UPDATE users SET has_voted = TRUE WHERE name = ?");
                            pstmt.setString(1, currentUser);
                            pstmt.executeUpdate();

                            conn.commit();
                            JOptionPane.showMessageDialog(frame,
                                    "Thank you for voting! Your votes have been recorded.",
                                    "Voting Complete", JOptionPane.INFORMATION_MESSAGE);
                            frame.dispose();
                            createLoginWindow();
                        } catch (SQLException ex) {
                            try {
                                conn.rollback();
                            } catch (SQLException ex2) {
                                ex2.printStackTrace();
                            }
                            ex.printStackTrace();
                            showErrorDialog(frame, "Error recording your vote: " + ex.getMessage());
                        } finally {
                            try {
                                conn.setAutoCommit(true);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });

            logoutButton.addActionListener(e -> {
                frame.dispose();
                createLoginWindow();
            });

            footerPanel.add(submitButton);
            footerPanel.add(logoutButton);
            frame.add(footerPanel, BorderLayout.SOUTH);

        } catch (SQLException ex) {
            ex.printStackTrace();
            showErrorDialog(frame, "Error loading candidates: " + ex.getMessage());
            frame.dispose();
            createLoginWindow();
        }

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void createAdminDashboard() {
        JFrame frame = new JFrame("RTU Electronic Voting System - Admin Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("ADMINISTRATOR DASHBOARD");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeLabel.setForeground(new Color(200, 200, 200));
        headerPanel.add(welcomeLabel, BorderLayout.EAST);

        frame.add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(new Color(240, 240, 240));
        tabbedPane.setForeground(PRIMARY_COLOR);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        // Results Tab
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBackground(Color.WHITE);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea resultsArea = new JTextArea();
        resultsArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        resultsArea.setEditable(false);
        resultsArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane resultsScrollPane = new JScrollPane(resultsArea);
        resultsScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);

        JPanel resultsButtonPanel = new JPanel();
        resultsButtonPanel.setBackground(Color.WHITE);
        resultsButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton refreshResultsButton = createStyledButton("Refresh Results", Color.WHITE, PRIMARY_COLOR);
        JButton printResultsButton = createStyledButton("Print Results", Color.WHITE, PRIMARY_COLOR);

        refreshResultsButton.addActionListener(e -> {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT position, name, votes FROM candidates ORDER BY position, votes DESC");

                StringBuilder sb = new StringBuilder();
                String currentPosition = null;

                while (rs.next()) {
                    String position = rs.getString("position");
                    if (!position.equals(currentPosition)) {
                        if (currentPosition != null) sb.append("\n");
                        sb.append("=== ").append(position.toUpperCase()).append(" ===\n\n");
                        currentPosition = position;
                    }
                    sb.append(String.format("%-25s", rs.getString("name")))
                            .append(": ").append(rs.getInt("votes")).append(" votes\n");
                }

                resultsArea.setText(sb.toString());
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrorDialog(frame, "Error loading results: " + ex.getMessage());
            }
        });

        printResultsButton.addActionListener(e -> {
            try {
                resultsArea.print();
            } catch (Exception ex) {
                showErrorDialog(frame, "Error printing results: " + ex.getMessage());
            }
        });

        resultsButtonPanel.add(refreshResultsButton);
        resultsButtonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        resultsButtonPanel.add(printResultsButton);
        resultsPanel.add(resultsButtonPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Election Results", resultsPanel);

        // Manage Candidates Tab
        JPanel candidatesPanel = new JPanel(new BorderLayout());
        candidatesPanel.setBackground(Color.WHITE);
        candidatesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultListModel<String> candidatesListModel = new DefaultListModel<>();
        JList<String> candidatesList = new JList<>(candidatesListModel);
        candidatesList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        candidatesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        candidatesList.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JScrollPane candidatesScrollPane = new JScrollPane(candidatesList);
        candidatesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        candidatesPanel.add(candidatesScrollPane, BorderLayout.CENTER);

        JPanel candidatesButtonPanel = new JPanel();
        candidatesButtonPanel.setBackground(Color.WHITE);
        candidatesButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton addCandidateButton = createStyledButton("Add Candidate", Color.WHITE, PRIMARY_COLOR);
        JButton removeCandidateButton = createStyledButton("Remove Selected", Color.WHITE, PRIMARY_COLOR);
        JButton refreshCandidatesButton = createStyledButton("Refresh List", Color.WHITE, PRIMARY_COLOR);

        addCandidateButton.addActionListener(e -> showAddCandidateDialog(frame, refreshCandidatesButton));
        removeCandidateButton.addActionListener(e -> removeSelectedCandidate(frame, candidatesList, refreshCandidatesButton));
        refreshCandidatesButton.addActionListener(e -> refreshCandidatesList(candidatesListModel, frame));

        candidatesButtonPanel.add(addCandidateButton);
        candidatesButtonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        candidatesButtonPanel.add(removeCandidateButton);
        candidatesButtonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        candidatesButtonPanel.add(refreshCandidatesButton);
        candidatesPanel.add(candidatesButtonPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Manage Candidates", candidatesPanel);

        // Reset Election Tab
        JPanel resetPanel = new JPanel();
        resetPanel.setBackground(Color.WHITE);
        resetPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        resetPanel.setLayout(new BoxLayout(resetPanel, BoxLayout.Y_AXIS));

        JLabel resetLabel = new JLabel("Election Management Tools");
        resetLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        resetLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetPanel.add(resetLabel);
        resetPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JButton resetVotesButton = createStyledButton("Reset All Votes", Color.WHITE, PRIMARY_COLOR);
        resetVotesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetVotesButton.setMaximumSize(new Dimension(300, 40));
        resetVotesButton.addActionListener(e -> resetAllVotes(frame));

        JButton resetUsersButton = createStyledButton("Reset User Voting Status", Color.WHITE, PRIMARY_COLOR);
        resetUsersButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetUsersButton.setMaximumSize(new Dimension(300, 40));
        resetUsersButton.addActionListener(e -> resetUserVotingStatus(frame));

        resetPanel.add(resetVotesButton);
        resetPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        resetPanel.add(resetUsersButton);
        tabbedPane.addTab("Election Management", resetPanel);

        // Logout Panel with gold background and black text
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.setBackground(SECONDARY_COLOR);
        logoutPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 20));

        JButton logoutButton = createStyledButton("Logout", SECONDARY_COLOR, Color.BLACK);
        logoutButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 25, 8, 25)
        ));
        logoutButton.addActionListener(e -> {
            frame.dispose();
            createLoginWindow();
        });
        logoutPanel.add(logoutButton);

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(logoutPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Load initial data
        refreshResultsButton.doClick();
        refreshCandidatesButton.doClick();
    }

    private static void showAddCandidateDialog(JFrame parent, JButton refreshButton) {
        JDialog dialog = new JDialog(parent, "Add New Candidate", true);
        dialog.setSize(500, 500); // Increased height for additional fields
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        JLabel titleLabel = new JLabel("ADD NEW CANDIDATE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(25, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Name Field
        JLabel nameLabel = new JLabel("Candidate Name:");
        nameLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(nameLabel, gbc);

        JTextField nameField = new JTextField();
        nameField.setFont(LABEL_FONT);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(nameField, gbc);

        // Position Field
        JLabel positionLabel = new JLabel("Position:");
        positionLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(positionLabel, gbc);

        JComboBox<String> positionCombo = new JComboBox<>();
        positionCombo.setFont(LABEL_FONT);
        positionCombo.addItem("President");
        positionCombo.addItem("Vice President");
        positionCombo.addItem("Secretary");
        positionCombo.addItem("Treasurer");
        positionCombo.addItem("Other...");
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(positionCombo, gbc);

        // Custom Position Field
        JTextField customPositionField = new JTextField();
        customPositionField.setFont(LABEL_FONT);
        customPositionField.setVisible(false);
        gbc.gridx = 1;
        gbc.gridy = 2;
        inputPanel.add(customPositionField, gbc);

        positionCombo.addActionListener(e -> {
            if ("Other...".equals(positionCombo.getSelectedItem())) {
                customPositionField.setVisible(true);
            } else {
                customPositionField.setVisible(false);
            }
            dialog.pack();
        });

        // Role/Duties Field
        JLabel roleLabel = new JLabel("Platform:");
        roleLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(roleLabel, gbc);

        JTextArea roleArea = new JTextArea(3, 20);
        roleArea.setFont(LABEL_FONT);
        roleArea.setLineWrap(true);
        roleArea.setWrapStyleWord(true);
        JScrollPane roleScrollPane = new JScrollPane(roleArea);
        gbc.gridx = 1;
        gbc.gridy = 3;
        inputPanel.add(roleScrollPane, gbc);

        // Image Upload Field
        JLabel imageLabel = new JLabel("Candidate Image:");
        imageLabel.setFont(LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(imageLabel, gbc);

        JPanel imagePanel = new JPanel(new BorderLayout());
        JButton browseButton = createStyledButton("Browse...", Color.WHITE, PRIMARY_COLOR);
        JLabel imagePathLabel = new JLabel("No image selected");
        imagePathLabel.setFont(LABEL_FONT);

        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".png") || f.isDirectory();
            }
            public String getDescription() {
                return "Image files (*.jpg, *.jpeg, *.png)";
            }
        });

        browseButton.addActionListener(e -> {
            int returnValue = fileChooser.showOpenDialog(dialog);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                imagePathLabel.setText(selectedFile.getName());
            }
        });

        imagePanel.add(browseButton, BorderLayout.WEST);
        imagePanel.add(imagePathLabel, BorderLayout.CENTER);
        gbc.gridx = 1;
        gbc.gridy = 4;
        inputPanel.add(imagePanel, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        JButton saveButton = createStyledButton("Save Candidate", SECONDARY_COLOR, PRIMARY_COLOR);
        JButton cancelButton = createStyledButton("Cancel", Color.WHITE, PRIMARY_COLOR);

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String position;

            if ("Other...".equals(positionCombo.getSelectedItem())) {
                position = customPositionField.getText().trim();
            } else {
                position = positionCombo.getSelectedItem().toString().trim();
            }

            String role = roleArea.getText().trim();
            String imagePath = imagePathLabel.getText().equals("No image selected") ?
                    null : IMAGE_DIR + imagePathLabel.getText();

            if (!name.isEmpty() && !position.isEmpty()) {
                try {
                    // Copy image to application directory if one was selected
                    if (imagePath != null && !imagePathLabel.getText().equals("No image selected")) {
                        File source = fileChooser.getSelectedFile();
                        File dest = new File(imagePath);
                        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO candidates (name, position, role, image_path) VALUES (?, ?, ?, ?)");
                    pstmt.setString(1, name);
                    pstmt.setString(2, position);
                    pstmt.setString(3, role);
                    pstmt.setString(4, imagePath);
                    pstmt.executeUpdate();
                    dialog.dispose();
                    refreshButton.doClick();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showErrorDialog(dialog, "Error adding candidate: " + ex.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showErrorDialog(dialog, "Error copying image file: " + ex.getMessage());
                }
            } else {
                showErrorDialog(dialog, "Please enter at least name and position");
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static void removeSelectedCandidate(JFrame parent, JList<String> list, JButton refreshButton) {
        String selected = list.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "Remove candidate: " + selected + "?\nThis action cannot be undone.",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    int id = Integer.parseInt(selected.split(":")[0].trim());
                    PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM candidates WHERE id = ?");
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    refreshButton.doClick();
                } catch (SQLException | NumberFormatException ex) {
                    ex.printStackTrace();
                    showErrorDialog(parent, "Error removing candidate: " + ex.getMessage());
                }
            }
        } else {
            showErrorDialog(parent, "Please select a candidate to remove");
        }
    }

    private static void refreshCandidatesList(DefaultListModel<String> model, JFrame frame) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT id, name, position FROM candidates ORDER BY position, name");

            model.clear();
            while (rs.next()) {
                model.addElement(
                        rs.getInt("id") + ": " + rs.getString("name") + " - " + rs.getString("position"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showErrorDialog(frame, "Error loading candidates: " + ex.getMessage());
        }
    }

    private static void resetAllVotes(JFrame parent) {
        int confirm = JOptionPane.showConfirmDialog(parent,
                "This will reset ALL candidate votes to zero. Are you sure you want to continue?",
                "Confirm Reset", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("UPDATE candidates SET votes = 0");
                JOptionPane.showMessageDialog(parent,
                        "All votes have been reset to zero.",
                        "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrorDialog(parent, "Error resetting votes: " + ex.getMessage());
            }
        }
    }

    private static void resetUserVotingStatus(JFrame parent) {
        int confirm = JOptionPane.showConfirmDialog(parent,
                "This will allow ALL users to vote again. Are you sure you want to continue?",
                "Confirm Reset", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("UPDATE users SET has_voted = FALSE");
                JOptionPane.showMessageDialog(parent,
                        "All users can now vote again.",
                        "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrorDialog(parent, "Error resetting user voting status: " + ex.getMessage());
            }
        }
    }

    private static JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private static void showErrorDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}