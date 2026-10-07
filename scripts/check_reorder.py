#!/usr/bin/env python3
"""Release smoke test: favourite drag changes order and survives process death.

Run on a test emulator with Moneta installed, English or Italian, at least
three visible favourites, and sufficient screen space (e.g. 412 x 915 dp).
This intentionally changes favourite order in that test installation.
"""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], stderr=subprocess.STDOUT)


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/moneta-reorder-test.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/moneta-reorder-test.xml"))


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap(labels, required=True):
    for node in tree().iter("node"):
        if node.get("text") in labels:
            x, y = center(node)
            adb("shell", "input", "tap", str(x), str(y))
            return
    if required:
        raise AssertionError(f"Missing control: {labels}")


def currencies():
    return [node for node in tree().iter("node") if re.fullmatch(r"[A-Z]{3}", node.get("text", ""))]


def top():
    # A keyed LazyColumn may keep the moved first row visible: scroll back to top.
    rows = currencies()
    assert len(rows) >= 4, "Need base plus at least three visible favourites"
    x, y = center(rows[1])
    _, bottom = center(rows[-1])
    adb("shell", "input", "swipe", str(x), str(y + 20), str(x), str(bottom + 150), "300")


adb("shell", "am", "start", "-n", "it.bbnss.moneta/.MainActivity")
tap({"Currencies", "Valute"})
tap({"Reorder", "Riordina"}, required=False)
top()
before = currencies()
base = before[0].get("text")
original = [row.get("text") for row in before[1:]]
x, y = center(before[1])
_, destination = center(before[-1])
adb("shell", "input", "touchscreen", "draganddrop", str(x), str(y), str(x), str(destination + 30), "1000")
top()
after = currencies()
ordered = [row.get("text") for row in after[1:]]
assert after[0].get("text") == base, "Drag unexpectedly changed the base"
assert set(ordered) == set(original), "Drag lost or added favourites"
assert ordered != original, f"Drag did not change order: {original}"
adb("shell", "am", "force-stop", "it.bbnss.moneta")
adb("shell", "am", "start", "-n", "it.bbnss.moneta/.MainActivity")
tap({"Currencies", "Valute"})
restored = [row.get("text") for row in currencies()]
assert restored[0] == base
assert restored[1:] == ordered, f"Order did not survive process death: {restored}"
print(f"PASS: drag {original} -> {ordered}; base {base} and order preserved after process death")
