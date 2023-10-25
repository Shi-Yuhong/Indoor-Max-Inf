package dataStruct;

// 此类用于表示室内区域的一点
public class Point {
    private int pid; // 所属的分区id
    private double location_x; // x坐标
    private double location_y; // y坐标

    public Point(){
        pid = -1;
        location_x = -1;
        location_y = -1;
    }

    public Point(int pid, double location_x, double location_y) {
        this.pid = pid;
        this.location_x = location_x;
        this.location_y = location_y;
    }

    public void setPoint(int pid, double location_x, double location_y) {
        this.pid = pid;
        this.location_x = location_x;
        this.location_y = location_y;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public double getLocation_x() {
        return location_x;
    }

    public void setLocation_x(double location_x) {
        this.location_x = location_x;
    }

    public double getLocation_y() {
        return location_y;
    }

    public void setLocation_y(double location_y) {
        this.location_y = location_y;
    }

    @Override
    public String toString() {
        return "Point{" +
                "pid=" + pid +
                ", location_x=" + location_x +
                ", location_y=" + location_y +
                '}';
    }

    // 计算两点之间的欧氏距离
    public static double getEuclidDist(Point s,Point t){
        double x1 = s.getLocation_x();
        double x2 = t.getLocation_x();
        double y1 = s.getLocation_y();
        double y2 = t.getLocation_y();
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    public static double getEuclidDist(Point s,Door d){
        double x1 = s.getLocation_x();
        double x2 = d.getX();
        double y1 = s.getLocation_y();
        double y2 = d.getY();
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}
