# utils/img_utils.py
import cv2
import base64

def image_to_base64(frame):
    """将OpenCV图像帧转换为Base64字符串"""
    _, buffer = cv2.imencode('.jpg', frame, [int(cv2.IMWRITE_JPEG_QUALITY), 60])
    return base64.b64encode(buffer).decode('utf-8')


def compute_iou(box1, box2):
    """计算两个边界框的交并比 (IoU)，格式为 [x, y, w, h]"""
    x1, y1, w1, h1 = box1
    x2, y2, w2, h2 = box2

    b1_x1, b1_y1 = x1, y1
    b1_x2, b1_y2 = x1 + w1, y1 + h1
    b2_x1, b2_y1 = x2, y2
    b2_x2, b2_y2 = x2 + w2, y2 + h2

    inter_x1 = max(b1_x1, b2_x1)
    inter_y1 = max(b1_y1, b2_y1)
    inter_x2 = min(b1_x2, b2_x2)
    inter_y2 = min(b1_y2, b2_y2)

    inter_w = max(0, inter_x2 - inter_x1)
    inter_h = max(0, inter_y2 - inter_y1)
    inter_area = inter_w * inter_h

    b1_area = w1 * h1
    b2_area = w2 * h2

    union_area = b1_area + b2_area - inter_area
    if union_area == 0:
        return 0.0
    return inter_area / float(union_area)