# core/web_stream.py
import cv2
import threading
from flask import Flask, Response
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

global_frame = None
lock = threading.Lock()

def update_frame(frame):
    """供外界调用，更新最新画面"""
    global global_frame, lock
    with lock:
        global_frame = frame.copy() if frame is not None else None

def generate():
    global global_frame, lock
    while True:
        with lock:
            if global_frame is None:
                continue
            frame_resized = cv2.resize(global_frame, (640, 480))
            ret, buffer = cv2.imencode('.jpg', frame_resized, [int(cv2.IMWRITE_JPEG_QUALITY), 70])
            frame_bytes = buffer.tobytes()

        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

def start_flask():
    app.run(host='0.0.0.0', port=5000, debug=False, use_reloader=False)