# skills/base_skill.py
class BaseSkill:
    def run(self, frame):
        """
        处理图像帧的统一接口
        :return: is_alarm (bool), alarm_data (dict), processed_frame (numpy array)
        """
        raise NotImplementedError