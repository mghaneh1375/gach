import numpy as np
import cv2
import sys
import os
import argparse


ap = argparse.ArgumentParser()

ap.add_argument("-t", "--total", required=True, help="number of pages")
ap.add_argument("-p", "--prefix", required=True, help="prefix")
ap.add_argument("-o", "--output", required=True, help="output path")

args = vars(ap.parse_args())

prefix = args["prefix"]
pages = int(args["total"])
directory = os.getcwd() + "/"
base = directory + prefix
output_dir = args["output"]

for i in range(pages):
    print(base + "_" + str(i + 1) + ".jpg")
    img = cv2.imread(base + "_" + str(i + 1) + ".jpg")
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gray = 255*(gray < 128).astype(np.uint8) # To invert the text to white
    coords = cv2.findNonZero(gray) # Find all non-zero points (text)
    x, y, w, h = cv2.boundingRect(coords) # Find minimum spanning bounding box
    rect = img[y:y+h, x:x+w] # Crop the image - note we do this on the original image
    cv2.imwrite(output_dir + "out-" + str(i + 1) + ".jpg", rect) # Save the image
