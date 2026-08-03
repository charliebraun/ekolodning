import cv2
import numpy as np
import re
import os

MIN_GPS_ACCURACY = 25

import argparse

parser = argparse.ArgumentParser(
    description="Converts GPS location and video from the android app into a csv format that the visualizers accept"
)

parser.add_argument(
    "input",
    help="Name of the textfile which contains all the GPS locations"
)

def parse_time(line, time_started):
    m = re.search(r"Time:\s*(\d+):(\d+):(\d+)", line)
    if m:
        hour, minute, second = map(int, m.groups())
        return 3600*hour+60*minute+second - time_started
    return -1
       
args = parser.parse_args()

if __name__ == '__main__':
    data = []
    time_started = 0
    lines = []
    quitting = False
    skipping = False
    if not os.path.exists(f'{args.input}.log.txt') or not os.path.exists(f'{args.input}.mp4'):
        print('Input files does not exist! Please make sure that both the .log.txt and .mp4 file exists!')
        exit()
    video = cv2.VideoCapture(f"{args.input}.mp4")
    with open(f"{args.input}.log.txt") as f:
        _lines = f.read().split("\n")
        line_zero_parts = _lines[0].split(":")
        hour = int(line_zero_parts[1][-2:])
        minute = int(line_zero_parts[2])
        second = int(line_zero_parts[3][:2])
        time_started = 3600*hour+60*minute+second
        lines = _lines[1:-2]
    for line in lines:
        gps_accuracy = int(re.search(r"Acc:\s*(\d+)", line).groups()[0])
        if gps_accuracy > 20:
            continue
        time_ms = parse_time(line, time_started)*1000
        video.set(cv2.CAP_PROP_POS_MSEC, time_ms)
        ret, frame = video.read()
        if ret:
            text = ""
            while True:
                display = cv2.resize(frame, (0, 0), fx=0.25, fy=0.25).copy()
                cv2.putText(
                    display,
                    f"Label: {text}",
                    (20, 40),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1,
                    (0, 255, 0),
                    2,
                )
                
                cv2.putText(
                    display,
                    "Type digits, Enter=confirm, Backspace=delete, s=skip, q=quit",
                    (20, 80),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (255, 255, 255),
                    2,
                )
                
                cv2.imshow("Frame", display)
                key = cv2.waitKey(0)
                
                if key in (13, 10):
                    if text:
                        value = float(text)
                    break
                elif key in (8, 127):
                    text = text[:-1]
                elif ord('0') <= key <= ord('9'):
                    text += chr(key)
                elif key == ord('.') and '.' not in text:
                    text += '.'
                elif key == ord('q'):
                    quitting = True
                    break
                elif key == ord('s'):
                    skipping = True
                    break
        if quitting == True:
            break
        if skipping:
            skipping = False
            continue
        data.append(f"Depth: {value}, {line}")

    video.release()
    cv2.destroyAllWindows()
    print(data)
    with open(f"{args.input}.processed.txt", "w") as f:
        for d in data:
            f.write(f"{d}\n")

