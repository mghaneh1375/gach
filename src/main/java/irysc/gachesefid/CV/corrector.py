import sys
import argparse
import imutils
import cv2
import tests

DEV_MODE = False

width_bottom_thresh = 6
width_top_thresh = 50

height_bottom_thresh = 4
height_top_thresh = 30

left_thresh = 0.1
right_thresh = 0.9
bottom_thresh = 0.9

output_dir = ""


def line_intersection(line1, line2):

    xdiff = (line1[0][0] - line1[1][0], line2[0][0] - line2[1][0])
    ydiff = (line1[0][1] - line1[1][1], line2[0][1] - line2[1][1])

    def det(a, b):
        return a[0] * b[1] - a[1] * b[0]

    div = det(xdiff, ydiff)
    if div == 0:
        raise Exception('lines do not intersect')

    d = (det(*line1), det(*line2))
    x = det(d, xdiff) / div
    y = det(d, ydiff) / div
    return [int(round(x)), int(round(y))]


def recovery(rects, eyes, image, width, height):

    missed = eyes - len(rects)

    left_sides = []
    right_sides = []
    reminder = []

    for rect in rects:

        if rect[0] / width < left_thresh or rect[0] / width > right_thresh:

            if rect[0] < width / 2:
                if DEV_MODE:
                    cv2.rectangle(image, (rect[0], rect[1]), (rect[0] + rect[2], rect[1] + rect[3]), (0, 255, 0), 1)
                left_sides.append(rect)
            else:
                if DEV_MODE:
                    cv2.rectangle(image, (rect[0], rect[1]), (rect[0] + rect[2], rect[1] + rect[3]), (255, 0, 0), 1)
                right_sides.append(rect)
        else:
            reminder.append(rect)

    # if len(left_sides) != len(right_sides):

    if len(reminder) < eyes - (total_q_in_col * 2 + 6):

        sum_diffs = 0
        count_diffs = 0

        reminder.sort(key=lambda x: x[0])

        for i in range(0, len(reminder) - 1):
            sum_diffs += reminder[i + 1][0] - reminder[i][0]
            count_diffs += 1

        avg_diff = (sum_diffs - 5 * 50) / count_diffs

        for i in range(0, len(reminder) - 1):
            if reminder[i + 1][0] - reminder[i][0] > 1.5 * avg_diff:

                test_area_x = int(reminder[i][0] + avg_diff)
                area_sum = 0
                area_counter = 0

                for ii in range(test_area_x, test_area_x + reminder[i][2]):
                    for jj in range(reminder[i][1], reminder[i][1] + reminder[i][3]):
                        b, g, r = image[jj, ii]
                        area_sum += r * 0.2126 + g * 0.7152 + b * 0.0722
                        area_counter += 1

                if area_sum / area_counter < black_thresh:
                    rects.append([test_area_x, reminder[i][1], reminder[i][2], reminder[i][3]])
                    # rect = rects[len(rects) - 1]
                    # cv2.rectangle(image, (rect[0], rect[1]), (rect[0] + rect[2], rect[1] + rect[3]), (0, 255, 0), 1)
                    missed -= 1

                    if missed > 0:
                        return recovery(rects, eyes, image, width, height)
                    else:
                        return rects
    return rects


def extract_epsilon(cnts, should_be, image, width, height, main_image):

    e = 0.06
    best_practice = -1
    best_rects = None
    avg_rect_width = 0
    avg_rect_height = 0

    while e < 0.09:

        counter = 0
        rects = []
        all_width = 0
        all_height = 0

        for c in cnts:

            peri = cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, e * peri, True)

            if len(approx) >= 2:

                x, y, w, h = cv2.boundingRect(approx)

                if width_bottom_thresh <= w <= width_top_thresh and height_bottom_thresh <= h <= height_top_thresh and (x / float(width) < left_thresh or x / float(width) > right_thresh or y / float(height) >= bottom_thresh):

                    sum = 0
                    pixels = 0

                    for i in range(x, x + w):
                        for j in range(y, y + h):
                            # b, g, r = image[j, i]
                            # sum += r * 0.2126 + g * 0.7152 + b * 0.0722
                            sum += image[j, i]
                            pixels += 1

                    # print(sum / pixels)
                    if sum / pixels <= black_thresh:

                        if DEV_MODE:
                            cv2.rectangle(main_image, (x, y), (x + w, y + h), (255, 0, 0), 4)

                        all_width += w
                        all_height += h
                        counter += 1
                        rects.append([x, y, w, h])
                    # else:
                    #     cv2.rectangle(image, (x, y), (x + w, y + h), (0, 0, 255), 1)
                    #     print(sum / pixels)

        avg_rect_width = int(all_width / counter)
        avg_rect_height = int(all_height / counter)
        # print(str(counter) + " " + str(should_be))

        if DEV_MODE:
            # cv2.imshow("a", main_image)
            # cv2.waitKey()
            cv2.imwrite("a.jpg", main_image)

        if counter >= should_be:
            return [False, avg_rect_width, rects, avg_rect_height]

        if counter > best_practice:
            best_practice = counter
            best_rects = rects

        e += 0.005

    return [True, avg_rect_width, best_rects, avg_rect_height]


def run(image, final_result):

    width = image.shape[1]
    height = image.shape[0]

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    blurred = cv2.GaussianBlur(gray, (9, 9), 0)

    ret, thresh = cv2.threshold(blurred, 140, 255, cv2.THRESH_BINARY)

    # edged = cv2.Canny(blurred, 75, 150)
    edged = cv2.Canny(thresh, 130, 200)

    # image = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)

    # cv2.imshow("a", image)
    # cv2.waitKey()
    if DEV_MODE:
        cv2.imwrite("b.jpg", edged)
    # cv2.imshow("a", edged)
    # cv2.waitKey()

    # find contours in the edge map, then initialize
    # the contour that corresponds to the document

    cnts = cv2.findContours(edged.copy(), cv2.RETR_EXTERNAL,
                            cv2.CHAIN_APPROX_SIMPLE)

    # cnts, hierarchy = cv2.findContours(edged, cv2.RETR_EXTERNAL,
    #                         cv2.CHAIN_APPROX_SIMPLE)
    #
    # cv2.drawContours(image, cnts, -1, (0, 255, 0), 1)
    # cv2.imshow("a", image)
    # cv2.waitKey()

    cnts = imutils.grab_contours(cnts)

    if len(cnts) == 0:
        print("???")
        sys.exit(0)

    cnts = sorted(cnts, key=cv2.contourArea, reverse=True)

    epsilon = extract_epsilon(cnts, eyes, thresh, width, height, image)

    need_recovery = epsilon[0]
    rects = epsilon[2]
    avg_rect_width = epsilon[1]
    avg_rect_height = epsilon[3]

    last_rows = []

    rects.sort(key=lambda x: x[1])

    if need_recovery:
        rects = recovery(rects, eyes, image, width, height)
        rects.sort(key=lambda x: x[1])

    # if best_practice:
    #     print("err")
    #     sys.exit(1)

    hor_lines = []
    ver_lines = []

    left_sides = []
    right_sides = []

    del rects[0]
    del rects[0]
    del rects[0]
    del rects[0]

    first_row_h = rects[0][1]
    width_f = width * 1.0

    for rect in rects:

        if rect[0] / width_f < left_thresh or rect[0] / width_f > right_thresh:

            if rect[0] < width_f / 2:
                if DEV_MODE:
                    cv2.rectangle(image, (rect[0], rect[1]), (rect[0] + rect[2], rect[1] + rect[3]), (0, 255, 0), 1)
                left_sides.append(rect)
            else:
                if DEV_MODE:
                    cv2.rectangle(image, (rect[0], rect[1]), (rect[0] + rect[2], rect[1] + rect[3]), (255, 0, 0), 1)
                right_sides.append(rect)

        else:
            last_rows.append(rect)

    top_left_point = left_sides[0]
    bottom_left_point = left_sides[len(left_sides) - 1]
    if top_left_point[0] == bottom_left_point[0]:
        m = 0
    else:
        m = ((bottom_left_point[1] - top_left_point[1]) * 1.0) / (bottom_left_point[0] - top_left_point[0])

    for t in range(0, len(left_sides)):
        x0 = left_sides[t][0]
        y0 = left_sides[t][1]
        x1 = right_sides[t][0]
        y1 = right_sides[t][1]
        if DEV_MODE:
            cv2.line(image, (x0, y0),
                     (x1, y1), (0, 128, 0), thickness=1)
        hor_lines.append([x0, y0, x1, y1])

    for rect in last_rows:

        x0 = rect[0]
        y0 = rect[1]
        y1 = first_row_h - 30

        if m == 0:
            x1 = int(x0)
        else:
            x1 = int((y1 - y0) / m + x0)

        if DEV_MODE:
            cv2.line(image, (x0, y0),
                     (x1, y1),
                     (128, 0, 0), thickness=1)
        ver_lines.append([x0, y0, x1, y1])

    ver_lines.sort(key=lambda x: x[0])
    hor_lines.sort(key=lambda x: x[1])

    itr = 0
    student_answers = []
    corrects = 0
    in_corrects = 0

    for ans in ANSWER_KEY:

        h_line = hor_lines[itr % total_q_in_col]

        ans -= 1

        goal_intersect = None
        fill = False
        duplicate = False
        student_answers.append(0)
        current_status = 0

        start_check = int(itr / total_q_in_col) * choices
        padding = 3

        for kk in range(start_check, start_check + choices):

            if duplicate:
                break

            v_line = ver_lines[kk]

            choice = kk % choices

            try:

                intersect = line_intersection([[h_line[0], h_line[1]], [h_line[2], h_line[3]]],
                                 [[v_line[0], v_line[1]], [v_line[2], v_line[3]]])

                if choice == ans:
                    goal_intersect = intersect

                sum = 0
                couter = 0

                for i in range(intersect[0], intersect[0] + avg_rect_width):
                    for j in range(intersect[1], intersect[1] + avg_rect_height):
                        b, g, r = image[j, i]
                        sum += r * 0.2126 + g * 0.7152 + b * 0.0722
                        couter += 1

                if DEV_MODE:
                    cv2.rectangle(image, (intersect[0], intersect[1]), (intersect[0] + avg_rect_width, intersect[1] + avg_rect_height), (255, 0, 0),
                                  thickness=1)

                if sum / couter <= student_black_thresh:

                    if fill:
                        duplicate = True
                        if current_status == 1:
                            in_corrects += 1
                            corrects -= 1
                        student_answers[itr] = -1
                    else:
                        student_answers[itr] = choice + 1
                        fill = True
                        if ans == choice:
                            current_status = 1
                            corrects += 1
                            if DEV_MODE or 1 == 1:
                                cv2.rectangle(image, (intersect[0] + padding, intersect[1]), (intersect[0] +  padding + avg_rect_width, intersect[1] + avg_rect_height),
                                              (0, 255, 0), 1)
                        else:
                            current_status = -1
                            in_corrects += 1
                            if DEV_MODE or 1 == 1:
                                cv2.rectangle(image, (intersect[0] + padding, intersect[1]), (intersect[0] +  padding + avg_rect_width, intersect[1] + avg_rect_height),
                                              (0, 0, 255), 1)
            except Exception as e:
                if DEV_MODE:
                    print(e)
                else:
                    continue

        if DEV_MODE:
            if duplicate:
                first_col = ver_lines[start_check]
                last_col = ver_lines[start_check + 3]
                cv2.line(image, (first_col[2], h_line[1] + 3), (last_col[2] + avg_rect_width, h_line[1] + 3), (0, 0, 255), 3)

            if not fill and goal_intersect is not None:
                cv2.rectangle(image, (goal_intersect[0], goal_intersect[1]),
                              (goal_intersect[0] + avg_rect_width, goal_intersect[1] + avg_rect_height),
                              (255, 0, 0), 1)

        itr += 1

    if DEV_MODE:
        cv2.imwrite("myout.jpg", image)
        if ','.join([str(x) for x in student_answers]) == final_result:
            print("Yess")
        else:
            print("fail")
    else:
        cv2.imwrite(output_dir, image)

    print(','.join([str(x) for x in student_answers]) + "_" + str((corrects * 3 - in_corrects) / (len(ANSWER_KEY) * 3)))
    sys.exit(1)


def main(final_result):
    image = cv2.imread(inputfile)

    # if image.shape[1] > 800:
    basewidth = 1200.0
    hsize = int((basewidth * image.shape[0]) / image.shape[1])
    run(cv2.resize(image, (int(basewidth), hsize)), final_result)
    # else:
    #     run(image)


ap = argparse.ArgumentParser()

ap.add_argument("-i", "--image", required=True, help="path to the input image")
ap.add_argument("-e", "--eyes", required=True, help="eyes count")
ap.add_argument("-a", "--answers", required=True, help="answers")
ap.add_argument("-d", "--darkness", required=True, help="darkness")
ap.add_argument("-c", "--choices", required=True, help="darkness")
ap.add_argument("-t", "--total", required=True, help="darkness")
ap.add_argument("-o", "--output", required=True, help="output")

args = vars(ap.parse_args())

eyes = int(args["eyes"])
output_dir = args["output"]

darkness_percent = int(args["darkness"])

black_thresh = 100
student_black_thresh = 160

if darkness_percent == 2:
    black_thresh += 40
    student_black_thresh += 40

choices = int(args["choices"])
total_q_in_col = int(args["total"])

# define the answer key which maps the question number
# to the correct answer
ANSWER_KEY = args["answers"].split("-")
ANSWER_KEY = [int(numeric_string) for numeric_string in ANSWER_KEY]

inputfile = args["image"]
if DEV_MODE:
    path = inputfile.split("/")
    final_result = tests.tests[path[len(path) - 1].split(".")[0]]
    main(final_result)
else:
    main(None)

