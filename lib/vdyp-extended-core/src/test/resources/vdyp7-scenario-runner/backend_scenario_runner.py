import argparse
import os
import sys
from os import listdir
from os.path import isfile, isdir, join
import subprocess


def run(scenarios_folder, console_path):
    print('running ' + console_path + '\\vdyp7console.exe on scenarios in ' + scenarios_folder)

    scenarios = [f for f in listdir(scenarios_folder) if isdir(join(scenarios_folder, f)) and
                 f.startswith('scenario')]
    print('scenario list: ' + str(scenarios))

    for s in scenarios:
        print(s)
        parms_file_name = join(scenarios_folder, s, 'parms.txt')
        print('    parms_file_name: ' + parms_file_name)

        if not isfile(parms_file_name):
            print("    parameters file 'parms.txt' not found in " + s + ", skipping")
            continue

        command = [join(console_path, 'vdyp7console.exe'), '-p', parms_file_name]
        process = subprocess.run(command, capture_output=True)


if __name__ == '__main__':

    parser = argparse.ArgumentParser('Runs, using VDYP7Console, the scenarios found in the given folder')
    parser.add_argument("console_path", help='the path to vdyp7console.exe')
    parser.add_argument("scenarios_path", help="the folder containing the scenarios (folders named 'scenario*')")

    params = parser.parse_args(sys.argv[1:])

    run(params.scenarios_path, params.console_path)
