import cv2
from math import sqrt
import pyautogui
from PyQt5.QtWidgets import QApplication, QWidget, QTextEdit, QPushButton
from PyQt5.QtCore import Qt, QSize

points = []
prepoints = [(278, 160), (358, 162), (240, 189), (167, 240), (167, 308), (212, 346), (278, 307), (353, 307), (402, 250), (445, 116), (472, 143), (521, 145), (540, 231), (591, 158), (674, 157), (756, 158), (837, 156), (919, 157), (998, 157), (1079, 156), (1159, 156), (1386, 205), (1218, 303), (1097, 303), (1066, 303), (856, 303), (826, 303), (614, 303), (170, 376), (168, 442), (169, 512), (168, 580), (169, 645), (168, 713), (168, 782), (167, 846), (166, 912), (226, 926), (266, 894), (337, 894), (415, 892), (478, 895), (533, 895), (613, 895), (682, 893), (279, 937), (360, 937), (439, 936), (521, 937), (600, 937), (684, 936), (223, 445), (224, 513), (225, 580), (224, 649), (223, 714), (407, 715), (406, 646), (406, 580), (406, 510), (406, 446), (458, 457), (458, 602), (458, 631), (595, 388), (699, 390), (786, 390), (819, 428), (590, 696), (641, 755), (713, 579), (815, 580), (717, 648), (720, 715), (814, 714), (812, 650), (766, 741), (900, 753), (953, 485), (957, 387), (1049, 373), (1192, 364), (1212, 573), (1211, 730), (756, 891), (720, 906), (850, 891), (1303, 892), (844, 922), (925, 923), (1007, 922), (1085, 922), (1166, 924), (1247, 923), (1352, 923), (1414, 893), (1240, 893), (1167, 893), (1097, 894), (1023, 895), (1314, 771), (1412, 808), (1412, 713), (1375, 714), (1375, 649), (1435, 607), (1376, 577), (1375, 517), (1435, 514), (1436, 443), (1436, 380), (1436, 313), (1435, 243), (1375, 397), (1377, 440), (1391, 481), (1191, 543), (869, 578), (428, 344), (403, 395), (194, 615), (228, 784), (235, 911), (497, 916), (430, 614), (719, 780), (1236, 752), (1062, 907), (1366, 906), (1402, 614), (1342, 203), (1324, 320), (1204, 321), (718, 319), (720, 368), (719, 178)]
start = -1
end = -1

# 匹配图上原有的点
def match_point(x,y):
    global start,end,points
    for point in prepoints:
        dist = cv2.norm((x, y), point)
        if dist <= 5:
            if start==-1:
                start = prepoints.index(point)+1
            else:
                end = prepoints.index(point)+1
            points.pop()
            points.append(point)

# 计算两点之间的直线距离
def calculate_distance(point1, point2):
    x1, y1 = point1
    x2, y2 = point2
    distance = sqrt((x2 - x1)**2 + (y2 - y1)**2)
    return distance

# 获取用户点击的点坐标
def get_mouse_click(event, x, y, flags, param):
    global  image, text_edit
    if event == cv2.EVENT_LBUTTONDOWN:
        points.append((x, y))
        cv2.circle(image, (x, y), 3, (0, 255, 0), -1)  # 在点击的位置画一个小绿点
        cv2.imshow("Image", image)
        match_point(x,y)
        # 在文本框中显示当前点坐标
        #text_edit.append(f"({x}, {y})")

# 打开指定图片并获取用户点击的点
def get_user_points(img_path):
    global image, text_edit

    # 读取图片
    image = cv2.imread(img_path)

    # 创建可调整大小的窗口，并设置为全屏模式
    cv2.namedWindow("Image", cv2.WINDOW_NORMAL)
    cv2.setWindowProperty("Image", cv2.WND_PROP_FULLSCREEN, cv2.WINDOW_FULLSCREEN)

    # 将鼠标事件绑定到窗口上
    cv2.setMouseCallback("Image", get_mouse_click)

    # 创建PyQt5窗口
    app = QApplication([])
    widget = QWidget()
    widget.setWindowTitle("统计窗口")
    widget.setFixedSize(QSize(400, 600))
    widget.setWindowFlags(Qt.WindowStaysOnTopHint)  # 显示在最前面

    # 添加文本框和按钮
    text_edit = QTextEdit(widget)
    text_edit.setReadOnly(True)
    text_edit.setGeometry(10, 10, 380, 500)

    def show_result():
        global start,end
        distances, total_distance = calculate_line_distances(points)
        final = int(round(total_distance / 10))
        points.clear()
        text_edit.append(f"{start} {end} {final}")
        start = -1
        end = -1

    btn_exit = QPushButton("退出", widget)
    btn_exit.setGeometry(10, 520, 180, 70)
    btn_exit.clicked.connect(lambda: widget.close())

    btn_show_result = QPushButton("查看结果", widget)
    btn_show_result.setGeometry(200, 520, 190, 70)
    btn_show_result.clicked.connect(show_result)

    widget.show()

    while True:
        cv2.imshow("Image", image)
        key = cv2.waitKey(1) & 0xFF
        if key == ord("q"):  # 按下"q"键退出循环
            break


    return points

# 计算点按顺序连接的直线距离和总直线距离
def calculate_line_distances(points):
    line_distances = []
    total_distance = 0
    for i in range(len(points) - 1):
        point1 = points[i]
        point2 = points[i + 1]
        distance = calculate_distance(point1, point2)
        line_distances.append(distance)
        total_distance += distance
        cv2.line(image, point1, point2, (255, 0, 0), 2)  # 在图片上画直线
        cv2.imshow("Image", image)
        cv2.waitKey(100)  # 等待0.5秒以显示连线效果

    return line_distances, total_distance

# 示例使用
img_path = "img/color.jpg"  # 指定图片路径

get_user_points(img_path)
distances, total_distance = calculate_line_distances(points)

