#import cv2
import numpy as np
import re
#import argparse
#
#parser = argparse.ArgumentParser(
#    description="Converts GPS location and video from the android app into a csv format that the visualizers accept"
#)
#
#parser.add_argument(
#    "input",
#    help="Name of the textfile which contains all the GPS locations"
#)

#video = cv2.VideoCapture("test")

def parse_time(line, time_started):
    m = re.search(r"Time:\s*(\d+):(\d+):(\d+)", line)
    if m:
        hour, minute, second = map(m, m.groups())
       

if __name__ == '__main__':
    time_started = 0
    lines = []
    with open("series_20260727_162700.log.txt") as f:
        _lines = f.read().split("\n")
        line_zero_parts = _lines[0].split(":")
        hour = int(line_zero_parts[1][-2:])
        minute = int(line_zero_parts[2])
        second = int(line_zero_parts[3][:2])
        time_started = 3600*hour+60*minute+second
        lines = _lines[1:-1]
    


