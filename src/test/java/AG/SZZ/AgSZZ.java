package AG.SZZ;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgSZZ {

	public static void main(String[] args) {
//		String str1 = "// 		str1";
//		String str2 = "//* str2";
//		String str3 = "*/ 		str3";
//		String str4 = "*********str4";
//		String str5 = "* str5";
//		String str6 = "/////////str6";
//		String str7 = "this is not comment!";
//		String str8 = " //      str8";
//		String str9 = " /** 	str9";
//		String str10 = "str10 */";
//		String str11 = "str11 ***/";

//		str1 = str1.replaceAll( "[*]*/.*|[*]*|/.*|//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1" );

		
		String str1 = "				  	 	 		    	     		  	 			   					";
		String str2 = "  int i    	 =	0;";
		String str3 = "    import     java.util.regex.Pattern;";
		
		
		/**
		 * whitespace 01
		 */
		
		str1 = str1.replaceAll("\\s+", "");
		if(str1.equals("")) {
			System.out.println("This is not BIC! This string is whitespace.\n");
		} else {
			System.out.println("This string is available as a BIC candidate.\n");
		}
		
		
		/**
		 * whitespace 02
		 */
		
		boolean isBIC = false;

	    Pattern pattern = Pattern.compile("\\S");
	    Matcher matcher = pattern.matcher(str2);
	        
	    while (matcher.find()) {
	    	isBIC = true;			//찾으면 공백이 아니라는 뜻    
	    	break;
	    }
	        
	    if(isBIC) {
	    	System.out.println("This string is available as a BIC candidate.\n");
	    } else {
	    	System.out.println("This is not BIC! This string is whitespace.\n");
	    }
		
	
		/**
		 * import 
		 */
	    
		if(str3.trim().split(" ")[0].equals("import")) {
			System.out.println("This is not BIC! This is import statement.\n");
		} else{
			System.out.println("This string is available as a BIC candidate.\n");
		}
		
		
	}

}
