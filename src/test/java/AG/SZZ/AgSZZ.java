package AG.SZZ;

import java.util.HashMap;

public class AgSZZ {

	public static void main(String[] args) {
		Person person1;
		Person person2;
		int[] ageArr = {1,2,3};
		
		
		HashMap<Person, String> list = new HashMap<>();
		
		for(int i = 0; i < ageArr.length - 1; i++) {
			person1 = new Person("SJ", ageArr[i]);
			person2 = new Person("JY", ageArr[i + 1]);
//			person1 = new Person("Kimseokjin", 23);
			
//			person2 = person1;
//			person1 = person2;
			
			list.put(person1, Integer.toString(i));
			list.put(person2, Integer.toString(i+1));
		}
		
		System.out.println("size: "+list.size());
		
		
//		for(int i = 0; i < list.size(); i++) {
//			
//			
//		}

	}

}
