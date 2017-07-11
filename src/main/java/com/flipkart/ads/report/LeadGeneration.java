package com.flipkart.ads.report;

//apache poi imports


import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by rahul.sachan on 28/06/17.
 */
public class LeadGeneration {

	public static void main(String[] args)
	{
		try(Workbook wb = new XSSFWorkbook())
		{
			POIFSFileSystem fileSystem = new POIFSFileSystem();
			Sheet sheet = wb.createSheet();
			Row r = sheet.createRow(0);
			Cell cell = r.createCell(0);
			cell.setCellType(Cell.CELL_TYPE_STRING);
			cell.setCellValue("Test");

				EncryptionInfo info = new EncryptionInfo(EncryptionMode.standard);
				Encryptor enc = info.getEncryptor();
				enc.confirmPassword("pass");
				OutputStream encryptedDS = enc.getDataStream(fileSystem);
				wb.write(encryptedDS);
				FileOutputStream fos = new FileOutputStream("/Users/rahul.sachan/Downloads/example.xlsx");
			fileSystem.writeFilesystem(fos);
				fos.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}




}






