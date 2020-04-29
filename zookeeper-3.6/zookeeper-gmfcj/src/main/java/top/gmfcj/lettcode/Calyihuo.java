package top.gmfcj.lettcode;

import java.util.Arrays;

/**
 * 一个整型数组 nums 里除两个数字之外，其他数字都出现了两次。
 * 请写程序找出这两个只出现一次的数字。要求时间复杂度是O(n)，空间复杂度是O(1)。
 * 位运算
 * (a^b)^c = c^(a^b)
 * a^0 = a
 * a^a = 0
 * a&(-a)=a
 * -a & a = a&(a的反码+1)
 * 7 & -7
 * 7    0111
 * -7   1001
 * 0001
 */
public class Calyihuo {
    public static void main(String[] args) {
        int flag = (-7) & 7;
        System.out.println((flag & 1) == 0);
        Calyihuo calyihuo = new Calyihuo();
//        calyihuo.singleNumbers(new int[]{1,2,10,4,1,4,3,3});
        calyihuo.singleNumbers(new int[]{1,2,3,4,1,2,5,5});
        System.out.println(calyihuo.sumNums(4));
    }

    int result = 0;

    public int sumNums(int num) {
        boolean x = num > 1 && sumNums(num - 1) > 0;
        result += num;
        return result;
    }

    public int[] singleNumbers(int[] nums) {
        // 数组中只有两个数不重复
        int[] result = new int[2];
        int sum = 0;
        for (int i = 0; i < nums.length; i++) {
            sum ^= nums[i];
        }
        // sum
        int flag = (-sum) & sum;
        // 不重复的两个值  一个和flag &是0，一个和flag &是1
        for (int num : nums) {
            if ((flag & num) == 0) {
                result[0] ^= num;
            } else {
                result[1] ^= num;
            }
        }
        System.out.println(Arrays.toString(result));
        return result;
    }
}
