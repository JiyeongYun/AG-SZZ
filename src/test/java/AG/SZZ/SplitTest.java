package AG.SZZ;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SplitTest {

	public static void main(String[] args) {
		String str = "public class HelloWorldLineFeed{\n" + 
				"    System.out.println(\"Hello\");\n" + 
				"\n" + 
				"}\n" + 
				"\n" + 
				"";
		
		
		Pattern pattern = Pattern.compile("\r\n|\r|\n");
	    Matcher matcher = pattern.matcher(str);
			
		String[] arr = str.split("\r\n|\r|\n");
		
		int lineCnt = 1;
		
		while (matcher.find()) {
			lineCnt ++;
		}
		
		System.out.println("Line Count : " + lineCnt);
		System.out.println("Length : " + arr.length);
		for(int i = 0; i < arr.length; i++) {
			System.out.println(arr[i]);
		}
	}
}
