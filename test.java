import java.util.Scanner;

public class test {
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        int val = scanner.nextInt();
        int length = ((val)/3);
        int startPoint1 = 0;
        int endPoint1 = length;
        int startPoint2 = endPoint1+1;
        int endPoint2 = endPoint1+length;
        int startPoint3 = endPoint2+1;
        int endPoint3 = endPoint2+length;
        if(endPoint3>val){
            endPoint3 = endPoint3 - (endPoint3 - val);
        }else if(endPoint3<val){
            endPoint3 = endPoint3 + (val - endPoint3);
        }
        System.out.println(startPoint1);
        System.out.println(endPoint1);
        System.out.println(startPoint2);
        System.out.println(endPoint2);
        System.out.println(startPoint3);
        System.out.println(endPoint3);
    }
}
