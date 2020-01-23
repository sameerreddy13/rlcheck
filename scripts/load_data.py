import os
import numpy as np

class DataLoader:

	generators = ['quickcheck', 'zest', 'rl']
	validity = ['ant', 'maven', 'closure', 'rhino']

	sm = {
		0: 'unix_time',
		1: 'unique_crashes',
		2: 'total_cov',
		3: 'valid_cov',
		4: 'total_inputs',
		5: 'valid_inputs',
		6: 'valid_paths',
		7: 'valid_branch_sets',
        8: 'unique_valids',
	}
	reverse_entries = [(kv[1], kv[0]) for kv in sm.items()]
	sm.update(reverse_entries) 

	def __init__(self, E):
		'''
        E: base directory for experiments
		Data dict becomes dictionary such that

		data_dict[{experiment name}] = numpy array with entry i equal to data for one run 
									length = num runs and 
									entry shape of num_lines x 7

		data_dict has dictionary entry for each experiment
		'''
		self.E = E 
		self.data_dict = {}


	def load_data(self):
		for d in os.listdir(self.E):
			if '.' in d: continue
			data = []
			target = self.join(self.E, d)
			run_data = self.parse_plot_data(target)
			data.append(np.array(run_data, dtype=float))

			self.data_dict[d] = np.array(data)



	def get_data(self, base_dirname, replay):
		'''
		gen = one of quickcheck, zest, rl
		validity = one of ant, maven, closure, rhino
		'''
		dirnames = [base_dirname + "-{}{}".format(i, "-replay" if replay else "") for i in range(0,10) ]
		data = [v[0] for d,v in self.data_dict.items() if d in dirnames]
		return data

	def join(self, *args):
		return "/".join(args)

	def parse_plot_data(self, target):
		p = self.join(target, 'plot_data')

		with open(p, 'r') as fp:
			lines = fp.readlines()
			del lines[0]
			lines = lines[0:len(lines):len(lines)//100][:100] # TODO 
		results = []
		for l in lines:
			l = str(l).replace('\n', "")
			stats = [int(x) for x in l.split(',')]
			for i, s in enumerate(stats):
				try:
					results[i].append(s)
				except IndexError:
					results.append([s])
		return np.array(results)

