package AG.SZZ;

public class StringTest {

	public static void main(String[] args) {
//		String str = "";
//		String str2 = null;
		String[] arr = new String[10];
		
		// set
		for(int i = 0; i < arr.length; i++) {
			arr[i] = "";
		}
		
//		System.out.println("Name 1 : " + str.getClass().getName());
//		System.out.println("Name 2 : " + str2.getClass().getName());
		for(int i = 0; i < arr.length; i++) {
			System.out.println("Class : " + arr[i].getClass().getName());
			System.out.println("content : " + arr[i]);
		}
	}
}
