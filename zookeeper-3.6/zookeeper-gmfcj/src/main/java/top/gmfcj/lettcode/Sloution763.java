package top.gmfcj.lettcode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * [2,3,1,1,4]  len-1=4  indexSum = 4
 * f(0) = 0
 * f(1) = max{ 1,2,3...,a[0] } = max{1,2}
 * f(2) = max{ 1+a[1] / 2+a[2] / ... / a[0]+a[a[0]] }=max{1+1,1+2,1+3,2+1}=max{2,3,4,3}
 * f(3) = max{f(2)+arr[f(2)]} = max{4+4, 3+1}
 * ...
 * f(i) = max{ f(i-1)+arr[i-1] }
 */
public class Sloution763 {


    public static void main(String[] args) {
//        String s = "ababcbacadefegdehijhklij";
//        partitionLabels(s);
//        jump1(new int[]{2, 3, 1, 1, 4});
//        canJump2(new int[]{3, 2, 1, 0, 4});
//        findMaxFrom(new String[]{"10", "0001", "111001", "1", "0"}, 5,3);
        findMaxFrom(new String[]{"10", "0","1"}, 1,1);
    }

    // [2,3,1,1,4] 局部最优解 -> 全局最优解  每次找返回内可用跳最远的路径[2,1,3,1]
    public static void jump1(int[] nums) {
        int end = 0;
        int maxPosition = 0;
        int steps = 0;
        // length-1保证不会在最后一位进行起跳
        for (int i = 0; i < nums.length - 1; i++) {
            // 找能跳到最远的
            maxPosition = Math.max(maxPosition, nums[i] + i);
            // 遇到边界，就更新边界，并且将步数+1  第一次起跳或者是到了边界
            if (i == end) {
                end = maxPosition;
                steps++;
            }
        }
        System.out.println("最少需要 => " + steps);
    }

    // [2,3,1,1,4]  [3,2,1,0,4]
    public static void canJump1(int[] nums) {
        int maxPosition = 0;
        for (int i = 0; i < nums.length; i++) {
            // 最大的位置
            maxPosition = Math.max(maxPosition, nums[i] + i);
            if (maxPosition < nums.length - 1
                    && i == maxPosition
                    && nums[maxPosition] == 0) {
                // 无法跳出
                System.out.println("无法跳到最后");
                return;
            }
        }
        System.out.println("can jump end");
    }

    /**
     * strs = ["10", "0001", "111001", "1", "0"], m = 5, n = 3  => 4 => {"10","0001","1","0"}
     * @param strs 字符串数组
     * @param m 0的个数
     * @param n 1个个数
     */
    public static void findMaxFrom(String[] strs, int m, int n) {
        // 每个子串的最大长度是600
        ArrayList<ArrayList<String>> as = new ArrayList<ArrayList<String>>(100);
        for (int i = 0; i < 100; i++) {
            as.add(new ArrayList<>(16));
        }
        for (int i = 0; i < strs.length; i++) {
            // 根据位数进行排列
            ArrayList<String> list = as.get(strs[i].length() - 1);
            if (list == null){
                list = new ArrayList<>();
            }
            list.add(strs[i]);
        }
        // 取数
        List<String> subList = new ArrayList<>();
        for (int i = 0; i < as.size(); i++) {
            // 有i位的list
            ArrayList<String> arrayList = as.get(i);
            if(arrayList == null || arrayList.size() == 0){
                continue;
            }
            if (arrayList.get(0).length() > (m+n)) {
                break;
            }
            // 统计字符串中有多少个1和0
            for (String s : arrayList) {
                // 1
                int mc = 0;
                // 0
                int nc = 0;
                for (int j = 0; j < s.length(); j++) {
                    if (s.charAt(j) == '0') {
                        mc++;
                    } else {
                        nc++;
                    }
                }
                // 判断m n和mc nc的值
                if (mc <= m && nc <= n) {
                    // 当前的字符串是可以取出来的
                    subList.add(s);
                    m -= mc;
                    n -= nc;
                }
            }
        }
        subList.forEach(e -> System.out.println(e));
    }

    public static void canJump2(int[] nums) {
        int maxPosition = 0;
        for (int i = 0; i < nums.length; i++) {
            // 只计算在可达范围之内的步数
            if (i <= maxPosition) {
                // 最大的位置
                maxPosition = Math.max(maxPosition, nums[i] + i);
                if (maxPosition >= nums.length - 1) {
                    System.out.println("can jump end");
                    return;
                }
            }
        }
        System.out.println("无法跳到最后");

    }


    public static void partitionLabels(String s) {
        int[] last = new int[26];
        int len = s.length();
        for (int i = 0; i < len; i++) {
            last[s.charAt(i) - 'a'] = i;
        }
        List<Integer> partition = new ArrayList<>();
        int start = 0, end = 0;
        for (int i = 0; i < len; i++) {
            end = Math.max(end, last[s.charAt(i) - 'a']);
            if (i == end) {
                partition.add(end - start + 1);
                start = end + 1;
            }
        }
        for (Integer integer : partition) {
            System.out.println(integer);
        }
    }
}
