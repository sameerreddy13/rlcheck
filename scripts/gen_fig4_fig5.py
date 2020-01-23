
import re
import sys
from collections import Counter, namedtuple, defaultdict
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
plt.switch_backend('Agg')
## Make sure the plots are happy with type-3 fonts
import matplotlib
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
plt.rcParams.update({'font.size': 14})
plt.rcParams.update({'legend.fontsize': 12})
import sys
import os
import os.path

if len(sys.argv) != 2:
    print("Usage: {} results-dir".format(sys.argv[0]))
    sys.exit()
basedir = sys.argv[1]
if not os.path.isdir(basedir):
    print("Usage: {} results-dir".format(sys.argv[0]))
    print("ERROR: {} is not a directory".format(basedir))
    sys.exit()
else:
    try:
        os.mkdir(os.path.join(basedir, "figs"))
    except FileExistsError:
        # That's ok, we just wanted to create it in case it didn't exist.
        pass


valids_re = re.compile("([0-9]*) valids, ([0-9]*) unique valids")

Results = namedtuple('Results', ['name', 'counter', 'valids', 'uvalids'])


def process_file(f):
    all_results = []
    for line in f:
        if line.startswith("===="):
            cur_technique=line.rstrip().replace('=','')
        elif line.startswith("Counter"):
            muh_counts = eval(line)
            all_results.append(Results(cur_technique, muh_counts, num_valids, num_uniques))
        elif valids_re.search(line):
            m = valids_re.search(line)
            num_valids = int(m.group(1))
            num_uniques = int(m.group(2))
    return all_results

def valid_stats(result_list):
    unique_numbers = [result.valids for result in result_list]
    mean = np.mean(unique_numbers)
    stderr = np.std(unique_numbers)/np.sqrt(len(unique_numbers))
    return mean, stderr

def uvalid_stats(result_list):
    unique_numbers = [result.uvalids for result in result_list]
    mean = np.mean(unique_numbers)
    stderr = np.std(unique_numbers)/np.sqrt(len(unique_numbers))
    return mean, stderr


def counter_stats(result_list):
    counters = [result.counter for result in result_list]
    all_keys = {key for counter in counters for key in counter.keys()}
    result_stats = {}
    for key in all_keys:
        unique_numbers = [counter[key] for counter in counters]
        mean = np.mean(unique_numbers)
        stderr = np.std(unique_numbers)/np.sqrt(len(unique_numbers))
        result_stats[key] = (mean, stderr)
    return result_stats

def partition_by_technique(result_list):
    res = defaultdict(list)
    for result in result_list:
        res[result.name].append(result)
    return res

def print_nice(d):
    print(d.keys())
    print([v[0] for v in d.values()])


def make_valid_bar_plot(by_technique_results):

    colors_rgb = [ (240,228,66),(0,158,115) ]
    colors_rgb = [ (x[0]/256, x[1]/256, x[2]/256) for x in colors_rgb]
    hatches = ["..", "//"]
    width = 0.3
    offsets = [-1/2, 1/2]

    techniques = list(by_technique_results.keys())
    ind = np.arange(len(techniques))


    valid_means, valid_stderrs = zip(*[valid_stats(r) for r in by_technique.values()])
    uvalid_means, uvalid_stderrs = zip(*[uvalid_stats(r) for r in by_technique.values()])
    means = [valid_means, uvalid_means]
    stderrs = [valid_stderrs, uvalid_stderrs]
    labels = ["Valid", "Unique Valid"]

    fig, ax = plt.subplots(figsize=(7.5,4.5))
    ax.axhline(y=uvalid_means[-1],linestyle='--', color='k', alpha=0.5, zorder=1)
    for i in [0,1]:
            ax.bar(ind + width*offsets[i], means[i],
                                width, yerr=stderrs[i], label= labels[i],
                                            color=colors_rgb[i], edgecolor=[j/1.5 for j in colors_rgb[i]], hatch=hatches[i], zorder=2)
    ax.set_ylabel("Number of Generated Inputs")
    ax.set_xlabel("State Abstraction Method")
    ax.set_xticks(ind)
    ax.set_xticklabels(techniques)
    plt.legend(loc='upper left')
    plt.tight_layout()
    plt.savefig(os.path.join(basedir, "figs/Fig4.pdf"))

def make_lengths_line_plot(by_technique_results):
    techniques = list(by_technique.keys())
    xs = [list(counter_stats(r).keys()) for r in by_technique.values()]
    ys = [np.array([v[0] for v in counter_stats(r).values()]) for r in by_technique.values()]
    stds = [np.array([v[1] for v in counter_stats(r).values()]) for r in by_technique.values()]
    colors_rgb = [(86, 180, 233), (0,158,115), (240,228,66), (0,114,178)]
    colors_rgb = [ (x[0]/256, x[1]/256, x[2]/256) for x in colors_rgb]
    linestyles = [':', '-.', '--', '-']

    fig, ax = plt.subplots(figsize=(8.5, 4.5))
    for i in range(len(techniques)):
        plt.plot(xs[i], ys[i], label=techniques[i], linestyle=linestyles[i], color=colors_rgb[i], linewidth=2)
        plt.fill_between(xs[i], ys[i] + stds[i], ys[i] - stds[i], alpha=0.3, color=colors_rgb[i])
    plt.legend(loc='best')
    plt.xlabel("Tree Size")
    plt.ylabel("Avg. Unique Valid Inputs Generated")
    plt.tight_layout()
    plt.yscale('log')
    from matplotlib.ticker import FuncFormatter
    ax.yaxis.set_major_formatter(FuncFormatter(lambda y, _: '{:g}'.format(y)))
    plt.tight_layout()
    plt.savefig(os.path.join(basedir, "figs/Fig5.pdf"))


all_results = []
for root, _, files in os.walk(os.path.join(basedir, "python-data")):
    for name in files:
        filename = os.path.join(root, name)
        f = open(filename, 'r')
        results = process_file(f)
        all_results.extend(results)

by_technique = partition_by_technique(all_results)

make_valid_bar_plot(by_technique)
make_lengths_line_plot(by_technique)
