package com.flipkart.ad.dp;



import com.flipkart.ads.report.JDBCUtill;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.joda.time.*;


import javax.validation.constraints.Null;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;


/**
 * Created by rahul.sachan on 10/03/17.
 */

//ssh -f -N -L 7755:10.33.10.193:3306 10.32.246.118
//ssh -f -N -L 7766:10.32.149.12:3306 10.32.246.118

public class CampaignBilling {
    public static void main(String args[]) throws SQLException, IOException {
        System.out.println("CampaignBilling loaded");

        //get year and month for billing process
        String year = args[0];
        String month = args[1];
        String day = args[2];

        System.out.println(year + "="+ month +"="+ day);

        LocalDateTime currentTime = LocalDateTime.now();
        ConfigProvider cp = ConfigProvider.getInstance();
        String filename ;
        if (day != null && !day.isEmpty())  filename = "/tmp/billing_" + year + "_" + month +"_"+ day +".xls";
        else  filename = "/tmp/billing_" + year + "_" + month + ".xls";
        System.out.println(filename);

        FileOutputStream fileOut = new FileOutputStream(filename);
        HSSFWorkbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("campaigns");

        //Connection neoCon = DriverManager.getConnection("jdbc:mysql://127.0.0.1:7766/neo", "neo_ro", "ua1zg24j");
        //Connection reportcon = DriverManager.getConnection("jdbc:mysql://127.0.0.1:7755/neo", "demand", "demand123");

        Connection neoCon = DriverManager.getConnection(cp.get(Constant.NEO_DB_URL,""),cp.get(Constant.NEO_DB_USER,""),cp.get(Constant.NEO_DB_PWD,""));
        Connection reportcon = DriverManager.getConnection(cp.get(Constant.DB_CONFIGURATION_URL, ""), cp.get(Constant.DB_CONFIGURATION_USER, ""), cp.get(Constant.DB_CONFIGURATION_PASSWORD, ""));
        String exclusionList = cp.get(Constant.ACCOUNT_EXCLUSION_LIST,"");
        final String fields[] = {"BU", "Billed_From", "Billed_state_code", "Nature_of_Transac", "Shipped_From", "Shipped_From_state", "Billed_To", "Billed_To_State", "Billed_To_Address", "Billed_To_GSTIN", "Shipped_To", "Shipped_To_State", "Shipped_To_Address", "Shipped_To_GSTIN", "Business_Type", "Nature_of_transaction", "external_ro_id", "internal_ro_id", "PAN", "credit_limit", "credit_term", "HSN/SAC", "ITEM_TYPE", "ITEM_DESC", "start_date", "end_date", "QTY", "UOM", "Base_value", "taxable_value", "discount", "Document_Linkes", "POE_DOC", "RO_DOC","billing_gstin_doc","shipping_gstin_doc"};
        PreparedStatement pr;
        if (day != null && !day.isEmpty()) {
            String BILLQUERY = "select c.team_id,year,month,bannerid,sum(view) views,sum(gross_rev) Base_value,b.campaign_id,c.start_date,c.end_date,IF(c.cost_model='CPT','Days','Views') UOM,c.discount,c.name ITEM_DESC,c.total_budget,c.poe POE_DOC,ro.external_ro_id,ro.internal_ro_id,ro.ro_doc,ro.old_ro_id from (select year,month,bannerid,sum(view) view,sum(gross_rev) gross_rev from dp_daily_banner_metrics where year=? and month=? and day=? group by bannerid having gross_rev >0) r left join banner b on r.bannerid=b.id left join campaign c on b.campaign_id=c.id  left join release_order ro on c.ro_id=ro.id where c.team_id not in (?) group by b.campaign_id";
            System.out.println("daily query");
            pr = reportcon.prepareStatement(BILLQUERY);
            pr.setString(1, year);
            pr.setString(2, month);
            pr.setString(3, day);
            pr.setString(4,exclusionList);
        }else{
            String BILLQUERY = "select c.team_id,year,month,bannerid,sum(view) views,sum(gross_rev) Base_value,b.campaign_id,c.start_date,c.end_date,IF(c.cost_model='CPT','Days','Views') UOM,c.discount,c.name ITEM_DESC,c.total_budget,c.poe POE_DOC,ro.external_ro_id,ro.internal_ro_id,ro.ro_doc,ro.old_ro_id from (select year,month,bannerid,sum(view) view,sum(gross_rev) gross_rev from dp_daily_banner_metrics where year=? and month=? group by bannerid having gross_rev >0) r left join banner b on r.bannerid=b.id left join campaign c on b.campaign_id=c.id  left join release_order ro on c.ro_id=ro.id where c.team_id not in (?) group by b.campaign_id";
            System.out.println("Monthly query");
            pr = reportcon.prepareStatement(BILLQUERY);
            pr.setString(1, year);
            pr.setString(2, month);
            pr.setString(3,exclusionList);
        }

        ResultSet rs = pr.executeQuery();

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < fields.length; i++) {
            Cell fieldCell = headerRow.createCell(i);
            fieldCell.setCellValue(fields[i]);
        }

        int rownumber = 1;
        while (rs.next()) {
            Row row = sheet.createRow(rownumber++);

            String teamId = rs.getString("team_id");
            String SQUERY = "select CONCAT('Agreement:',IFNULL(b.agreement,''),'\\nOnboarding:',IFNULL(b.onboarding_form,''),'\\nCheque:',IFNULL(b.cancel_cheque,''),'\\nPan:',IFNULL(b.pan_doc,'')) Document_Linkes, b.*,concat(ad.line_one,' ',ad.line_two,' ',ad.line_three,',',ad.city,',',ad.pincode,',',ad.state,',',ad.country) Billed_To_Address, ad.state, concat(sadd.line_one,' ',sadd.line_two,' ',sadd.line_three,',',sadd.city,',',sadd.pincode,',',sadd.state,',',sadd.country) Shipped_To_Address, sadd.state Shipped_To_State from (select * from user_group ug where id = ( select parent_user_group from user_group where id=?)) a left join billing b on a.billing_id= b.billing_id left join address ad on b.billing_address_id= ad.id left join address sadd on b.shipping_address_id = sadd.id ";

            PreparedStatement pst = neoCon.prepareStatement(SQUERY);

            pst.setString(1, teamId);
            ResultSet rs1 = pst.executeQuery();
            rs1.next();

            Cell cell0 = row.createCell(0);
            cell0.setCellValue("FKMP");

            Cell cell1 = row.createCell(1);
            cell1.setCellValue("Flipkart Internet");

            Cell cell2 = row.createCell(2);
            cell2.setCellValue("KA");

            Cell cell3 = row.createCell(3);
            cell3.setCellValue("B2B/B2C");

            Cell cell4 = row.createCell(4);
            cell4.setCellValue("Flipkart Internet");

            Cell cell5 = row.createCell(5);
            cell5.setCellValue("KA");

            Cell cell6 = row.createCell(6);
            cell6.setCellValue(rs1.getString("billing_name"));

            Cell cell7 = row.createCell(7);
            cell7.setCellValue(rs1.getString("state"));

            Cell cell8 = row.createCell(8);
            cell8.setCellValue(rs1.getString("Billed_To_Address"));

            Cell cell9 = row.createCell(9);
            cell9.setCellValue(rs1.getString("billing_gstin_no"));

            Cell cell10 = row.createCell(10);
            cell10.setCellValue(rs1.getString("shipping_name"));

            Cell cell11 = row.createCell(11);
            cell11.setCellValue(rs1.getString("Shipped_To_State"));

            Cell cell12 = row.createCell(12);
            cell12.setCellValue(rs1.getString("Shipped_To_Address"));

            Cell cell13 = row.createCell(13);
            cell13.setCellValue(rs1.getString("shipping_gstin_no"));

            Cell cell14 = row.createCell(14);
            cell14.setCellValue("FKMP-brand ads");

            String ship_State = rs1.getString("Shipped_To_State");
            String bill_State = rs1.getString("state");
            String Nature_of_transaction = null;
            if (ship_State != null && !ship_State.isEmpty()) {
                if (ship_State.equalsIgnoreCase("KARNATAKA")) Nature_of_transaction = "Intra-state";
                else Nature_of_transaction = "Inter-state";
            } else if (bill_State != null && !bill_State.isEmpty()){
                if (bill_State.equalsIgnoreCase("KARNATAKA")) Nature_of_transaction = "Intra-state";
                else Nature_of_transaction = "Inter-state";
            }
            Cell cell15 = row.createCell(15);
            cell15.setCellValue(Nature_of_transaction);

            Cell cell16 = row.createCell(16);
            cell16.setCellValue(rs.getString("external_ro_id"));

            Cell cell17 = row.createCell(17);
            cell17.setCellValue(rs.getString("internal_ro_id"));

            Cell cell18 = row.createCell(18);
            cell18.setCellValue(rs1.getString("PAN"));

            Cell cell19 = row.createCell(19);
            cell19.setCellValue(rs1.getLong("credit_limit"));

            Cell cell20 = row.createCell(20);
            cell20.setCellValue(rs1.getString("credit_term"));

            Cell cell21 = row.createCell(21);
            cell21.setCellValue("SAC code");

            Cell cell22 = row.createCell(22);
            cell22.setCellValue("Service");

            Cell cell23 = row.createCell(23);
            cell23.setCellValue(rs.getString("ITEM_DESC"));

            Date start_date = rs.getDate("start_date");
            Cell cell24 = row.createCell(24);
            cell24.setCellValue(start_date);

            Date end_date = rs.getDate("end_date");
            Cell cell25 = row.createCell(25);
            cell25.setCellValue(end_date);

            String UOM = rs.getString("UOM");
            long duration = end_date.getTime() - start_date.getTime();

            DateTime dt1 = new DateTime(start_date);
            DateTime dt2 = new DateTime(end_date);

            Cell cell26 = row.createCell(26);
            if (UOM.equals("CPT"))
                cell26.setCellValue(Days.daysBetween(dt1, dt2).getDays());
            else
                cell26.setCellValue(rs.getInt("views"));


            Cell cell27 = row.createCell(27);
            cell27.setCellValue(UOM);

            Double Total_Budget = rs.getDouble("total_budget");
            Double Base_value = rs.getDouble("Base_value");
            if (Base_value > Total_Budget)
                Base_value = Total_Budget;

            Cell cell28 = row.createCell(28);
            cell28.setCellValue(Base_value);

            int discount = rs.getInt("discount");
            Double Taxable_Budget = Base_value * (1 - discount / 100);

            Cell cell29 = row.createCell(29);
            cell29.setCellValue(Taxable_Budget);

            Cell cell30 = row.createCell(30);
            cell30.setCellValue(discount);

            Cell cell31 = row.createCell(31);
            cell31.setCellValue(rs1.getString("Document_Linkes"));

            Cell cell32 = row.createCell(32);
            cell32.setCellValue(rs.getString("POE_DOC"));

            Cell cell33 = row.createCell(33);
            cell33.setCellValue(rs.getString("RO_DOC"));

            Cell cell34 = row.createCell(34);
            cell34.setCellValue(rs1.getString("billing_gstin_doc"));

            Cell cell35 = row.createCell(35);
            cell35.setCellValue(rs1.getString("shipping_gstin_doc"));
        }

        try {
            wb.write(fileOut);
            fileOut.flush();
            neoCon.close();
            reportcon.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            neoCon.close();
            reportcon.close();
        }
        fileOut.close();
    }

}
