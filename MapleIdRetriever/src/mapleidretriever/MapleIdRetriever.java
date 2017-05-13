/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mapleidretriever;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author RonanLana
 * 
 * This application acts two-way: first section sets up a table on the SQL Server with all the names used within MapleStory,
 * and the second queries all the names placed inside "fetch.txt", returning in the same line order the ids of the elements.
 * In case of multiple entries with the same name, multiple ids will be returned in the same line separated by a simple space
 * in ascending order. An empty line means that no entry with the given name in a line has been found.
 * 
 * IMPORTANT: this will fail for fetching MAP ID (you shouldn't be using this program for these, just checking them up in the
 * handbook is enough anyway).
 * 
 * Set whether you are first installing the handbook on the SQL Server (true) or just fetching whatever is on your "fetch.txt"
 * file (false) on the INSTALL_SQLTABLE property and build the project. With all done, run the Java executable.
 * 
 */
public class MapleIdRetriever {
    private final static boolean INSTALL_SQLTABLE = false;
    
    static String host = "jdbc:mysql://localhost:3306/maplesolaxia";
    static String driver = "com.mysql.jdbc.Driver";
    static String username = "root";
    static String password = "";
    
    static Connection con = null;
    static InputStreamReader fileReader = null;
    static BufferedReader bufferedReader = null;
    static PrintWriter printWriter = null;
    
    // ------- SET-UP section arguments --------
    
    static String directoryName = "./handbook/";
    
    // ------- SEARCH section arguments --------
    
    static String inputName = "fetch.txt";
    static String outputName = "result.txt";

    /**
     * @param args the command line arguments
     */
    
    private static void listFiles(String directoryName, ArrayList<File> files) {
            File directory = new File(directoryName);

            // get all the files from a directory
            File[] fList = directory.listFiles();
            for (File file : fList) {
                    if (file.isFile()) {
                            files.add(file);
                    } else if (file.isDirectory()) {
                            listFiles(file.getAbsolutePath(), files);
                    }
            }
    }
    
    private static void parseMapleHandbookLine(String line) throws SQLException {
        String[] tokens = line.split(" - ");
        
        if(tokens.length > 1) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO `handbook` (`id`, `name`) VALUES (?, ?)");
            ps.setInt(1, Integer.parseInt(tokens[0]));
            ps.setString(2, tokens[1]);
            ps.execute();
        }
    }
    
    private static void parseMapleHandbookFile(File fileObj) throws SQLException {
        String line;
        
        try {
            fileReader = new InputStreamReader(new FileInputStream(fileObj), "UTF-8");
            bufferedReader = new BufferedReader(fileReader);
            
            System.out.println("Parsing file '" + fileObj.getCanonicalPath() + "'.");

            while((line = bufferedReader.readLine()) != null) {
                parseMapleHandbookLine(line);
            }

            bufferedReader.close();
            fileReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    private static void setupSqlTable() throws SQLException {
        PreparedStatement ps = con.prepareStatement("DROP TABLE IF EXISTS `handbook`;");
        ps.execute();
        
        ps = con.prepareStatement("CREATE TABLE `handbook` ("
                + "`key` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                + "`id` int(10) DEFAULT NULL,"
                + "`name` varchar(200) DEFAULT NULL,"
                + "PRIMARY KEY (`key`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;");
        ps.execute();
    }
    
    private static void parseMapleHandbook() throws SQLException {
        ArrayList<File> files = new ArrayList<>();

        listFiles(directoryName, files);
        if(files.isEmpty()) return;

        setupSqlTable();

        for(File f: files) {
            parseMapleHandbookFile(f);
        }
    }
    
    public static void fetchDataOnMapleHandbook() throws SQLException {
        String line;
        
        try {
            fileReader = new InputStreamReader(new FileInputStream(inputName), "UTF-8");
            bufferedReader = new BufferedReader(fileReader);
            
            printWriter = new PrintWriter(outputName, "UTF-8");
            
            while((line = bufferedReader.readLine()) != null) {
                if(line.isEmpty()) {
                    printWriter.println("");
                    continue;
                }
                
                PreparedStatement ps = con.prepareStatement("SELECT `id` FROM `handbook` WHERE `name` LIKE ? COLLATE latin1_general_ci ORDER BY `id` ASC;");
                ps.setString(1, line);
                
                ResultSet rs = ps.executeQuery();
                
                String str = "";
                while(rs.next()) {
                    Integer id = rs.getInt("id");
                    
                    str += id.toString();
                    str += " ";
                }
                
                printWriter.println(str);
            }

            printWriter.close();
            bufferedReader.close();
            fileReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public static void main(String[] args) {
        
        try {
            Class.forName(driver).newInstance();
            con = DriverManager.getConnection(host, username, password);

            if(INSTALL_SQLTABLE) parseMapleHandbook();
            else fetchDataOnMapleHandbook();

            con.close();
        }
        
        catch(SQLException e) {
            System.out.println("Error: invalid SQL syntax");
            System.out.println(e.getMessage());
        }
        
        catch(ClassNotFoundException e) {
            System.out.println("Error: could not find class");
            System.out.println(e.getMessage());
        }

        catch(InstantiationException | IllegalAccessException e) {
            System.out.println("Error: instantiation failure");
            System.out.println(e.getMessage());
        }
    }
}
