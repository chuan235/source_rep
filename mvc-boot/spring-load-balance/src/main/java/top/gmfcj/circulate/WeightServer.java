package top.gmfcj.circulate;

/**
 * 保存server的当前权重和原始权重
 */
public class WeightServer {

    private String ip;
    private Integer currentWeight;
    private Integer weight;

    public WeightServer(String ip, Integer currentWeight, Integer weight) {
        this.ip = ip;
        this.currentWeight = currentWeight;
        this.weight = weight;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(Integer currentWeight) {
        this.currentWeight = currentWeight;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
