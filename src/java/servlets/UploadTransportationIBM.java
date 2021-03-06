/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import methods.dbUtils;
import methods.policy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@WebServlet(name = "UploadTransportationIBM", urlPatterns = {"/UploadTransportationIBM"})
@MultipartConfig
public class UploadTransportationIBM extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        URL oracle = new URL(request.getParameter("url").toString());
     //   System.out.println(request.getParameter("url").toString());
        Integer count_of_names = Integer.parseInt(request.getParameter("countofnames"));
        String tablename = request.getParameter("dbname");
        int ur = 0;
        int objectivecount = 0;

        List<Double> table2 = new ArrayList<>();
        List<String> pol_names = new ArrayList<>();
        List<policy> mypol = new ArrayList<>();
        String objn = "";
        String objp = "";
        String addobjp = "";
        String addobjn = "";
        String stpar = "";
        String[] criteria_names = new String[count_of_names];
        String line = "";
        String splitBy = ",";
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(oracle.openStream(), "UTF-8"));
            while ((line = r.readLine()) != null) {
                while (line.contains(",,")) {
                    line = line.replaceAll(",,", ",");
                }
                String[] policy = line.split(splitBy);
                String[] criteria_of_output = new String[count_of_names];
                policy ppp = new policy(objectivecount, count_of_names);
                if (ur == 0) {
                    for (int i = 0; i < policy.length; i++) {
                        if (i < count_of_names) {
                            String wget = policy[i].replaceAll(" ", "");
                            wget = wget.replaceAll("%", "");
                            wget = wget.replaceAll("-", "");
                            wget = wget.replaceAll("[(]", "_");
                            wget = wget.replaceAll("[)]", "_");
                            criteria_names[i] = wget;
                        } else {
                            String wget = policy[i].replaceAll(" ", "");
                            wget = wget.replaceAll("%", "");
                            wget = wget.replaceAll("-", "");
                            wget = wget.replaceAll("[(]", "_");
                            wget = wget.replaceAll("[)]", "_");
                            pol_names.add(wget);
                        }
                    }
                } else {
                    for (int i = 0; i < policy.length; i++) {
                        if (i < count_of_names) {
                            String wget = policy[i].replaceAll(" ", "");
                            wget = wget.replaceAll("%", "");
                            wget = wget.replaceAll("-", "");
                            criteria_of_output[i] = wget;
                        } else {
                            table2.add(Double.parseDouble(policy[i].replace(',', '.')));
                        }
                    }
                    objectivecount = policy.length - count_of_names;
                    int uobj = 0;
                    double[] objValues = new double[objectivecount];
                    for (double tab : table2) {
                        objValues[uobj] = tab;
                        uobj++;
                    }
                    ppp.setPolicyName("consensus_output_ERF-" + Integer.toString(ur));
                    ppp.setPolicyParameters(criteria_of_output);
                    ppp.setObjectives(objValues);
                    mypol.add(ppp);
                    table2.clear();
                }
                ur++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //    System.out.println(tablename);

        /*    for(int i=0;i<obj_names.length;i++){
         System.out.println(obj_names[i]);
         }
         */
        for (int i = 0; i < count_of_names; i++) {
            objp += "," + criteria_names[i] + " VARCHAR(255) ";
        }
        for (String obj : pol_names) {
            objn += "," + obj + " DOUBLE";
        }

        //PERASMA STI VASI TABLE1 
        String sql1 = "CREATE TABLE IF NOT EXISTS " + tablename
                + "(ID INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY "
                + " ,policy VARCHAR(255) "
                + objp
                + objn
                + ")";

        System.out.print(sql1);
        addobjn = createQM(pol_names.size());
        addobjp = createQM(count_of_names);
        Connection conn = dbUtils.getConnection();

        try {
            PreparedStatement statement = conn.prepareStatement(sql1);
            int exec = statement.executeUpdate();

            stpar += "ID,policy";
            for (int i = 0; i < count_of_names; i++) {
                stpar += "," + criteria_names[i] + "";
            }
            for (String obj : pol_names) {
                stpar += "," + obj + "";
            }

            String query = "INSERT INTO " + tablename + " (" + stpar + ") " + "VALUES(?,?" + addobjp + addobjn + " )";
            System.out.print(query);
            int pwi = 1;
            for (policy pol : mypol) {

                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, pwi);
                stmt.setString(2, pol.getPolicyName());
                for (int num = 0; num < count_of_names; num++) {
                    stmt.setString(num + 3, pol.getPolicyParameters()[num]);
                }
                for (int num = 0; num < pol_names.size(); num++) {
                    stmt.setDouble(num + count_of_names + 3, (double) Math.round(pol.getObjectives()[num] * 1000000) / 1000000);
                    //   System.out.print((double) Math.round(pol.getObjectives()[num] * 10000) / 10000);
                }
//                System.out.print(query);
                stmt.executeUpdate();
                pwi++;
            }
        } catch (SQLException e) {
            System.out.print("foooo");
        }
        dbUtils.closeConnection(conn);
        ServletContext context = getServletContext();
        RequestDispatcher dispatcher = context.getRequestDispatcher("/dbLoad.jsp");

        dispatcher.forward(request, response);

    }

 

    private static String createQM(int number) {
        String qm = "";
        for (int i = 0; i < number; i++) {
            qm += ",?";
        }
        return qm;
    }

    private static String readString(InputStream is) throws IOException {
        char[] buf = new char[2048];
        Reader r = new InputStreamReader(is, "UTF-8");
        StringBuilder s = new StringBuilder();
        while (true) {
            int n = r.read(buf);
            if (n < 0) {
                break;
            }
            s.append(buf, 0, n);
        }
        return s.toString();
    }
}
