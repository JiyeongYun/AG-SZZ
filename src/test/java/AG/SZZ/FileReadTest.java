package AG.SZZ;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileReadTest {

	public FileReadTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String filePath = "/Users/kimseokjin/git/JiraCrawler/apacheZEPPELIN/apacheZEPPELINIssueKeys2020-04-16 14:12:46.626.csv";
		
		try {
			List<String> issueKeys = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
			
	        for(String issueKey: issueKeys) {
	        	System.out.println(issueKey);
	        }
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
