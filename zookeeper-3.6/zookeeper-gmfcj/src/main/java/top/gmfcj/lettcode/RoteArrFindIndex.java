package top.gmfcj.lettcode;

/**
 * https://leetcode-cn.com/problems/search-in-rotated-sorted-array/
 * 3456721
 * 01234567
 */
public class RoteArrFindIndex {

    public static void main(String[] args) {
        RoteArrFindIndex obj = new RoteArrFindIndex();
//        int[] arr = new int[]{4, 5, 6, 7, 0, 1, 2};
        int[] arr = new int[]{6, 7, 0, 1, 2, 3, 4, 5};
//        System.out.println(new RoteArrFindIndex().search(arr, 0));
        int roteIndex = 0;
//        int index = obj.findRotePoint(arr, 0, arr.length - 1);
//        System.out.println("roteIndex=" + index);
//
//        System.out.println(obj.finBinaryIndex(arr, 5, 2, arr.length-1));

        System.out.println(obj.search(arr, 3));
    }

    public int search(int[] nums, int target) {
        // 找到旋转点
        int roteIndex = findRotePoint(nums, 0, nums.length - 1);
        System.out.println("找到旋转点索引：" + roteIndex);
        // 找到index
        if (target == nums[roteIndex]) {
            System.out.println("找到目标值索引：" + roteIndex);
            return roteIndex;
        } else if (target < nums[0]) {
            System.out.println("开始二分搜索,搜索值为："+target+"，索引搜索范围[" + (roteIndex + 1) + "," + (nums.length - 1) + "]");
            System.out.println("开始二分搜索,搜索值为："+target+"，数据搜索范围[" + nums[roteIndex + 1] + "," + nums[(nums.length - 1)] + "]");
            // (roteIndex,nums.length)
            return finBinaryIndex(nums, target, roteIndex + 1, nums.length - 1);
        } else if (target > nums[0]) {
            // (0, roteIndex)
            System.out.println("开始二分搜索，搜索范围[" + 0 + "," + roteIndex + "]");
            System.out.println("开始二分搜索，数据搜索范围[" + nums[0] + "," + nums[roteIndex] + "]");
            return finBinaryIndex(nums, target, 0, roteIndex);
        } else {
            System.out.println("未找到目标值索引，查询的值是：" + target);
            return -1;
        }
    }

    private int finBinaryIndex(int[] nums, int target, int start, int end) {
        // 升序的序列中使用二分法查找
        do {
            int mid = (start + end) / 2;
            if (target == nums[mid]) {
                return mid;
            } else if (target > nums[mid]) {
                start = mid + 1;
                continue;
            } else {
                end = mid - 1;
            }
        } while (start <= end);

        return -1;
    }

    private int findRotePoint(int[] nums, int start, int end) {
        int mid = (start + end) / 2;
        if (mid == 0 || mid == nums.length - 1) {
            return -1;
        }
        int index = mid;
        // 判断是否有序
        if (nums[mid - 1] < nums[mid] && nums[mid] < nums[mid + 1]) {
            index = findRotePoint(nums, start, mid);
            if (index != mid) {
                return index;
            }
            return findRotePoint(nums, mid, end);
        }
        // 无序
        return index;
    }

}
