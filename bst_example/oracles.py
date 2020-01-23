import random
import numpy as np
from numpy.random import binomial
from collections import defaultdict

class MockOracle:
    def Select(self, domain,idx):
        return random.choice(domain)

    def reward(self, r):
        pass

class Oracle:
    def Select(self, domain, idx):
        abstract_state = self.abstract_state_fn(self.choice_sequence)
        if not idx in self.learners:
            self.learners[idx] = Learner(self.epsilon, self.gamma, self.initial_val)
        choice = self.learners[idx].policy(domain, abstract_state)
        self.choice_sequence.append(choice)
        return choice

    def __init__(self, abstract_state_fn, epsilon=0.25, gamma=1.0, initial_val=0):
        self.abstract_state_fn = abstract_state_fn
        self.learners = {}
        self.choice_sequence = []
        self.epsilon = epsilon
        self.gamma = gamma
        self.initial_val = initial_val

    # updates upon full episodes
    def reward(self, reward):
        for learner in self.learners.values():
            learner.reward(reward)
        self.choice_sequence = []

class Learner:

    def __init__(self,  epsilon=0.25, gamma=1.0, initial_val=0):
        self.epsilon = epsilon
        self.gamma = gamma
        self.choice_state_sequence = []
        self.initial_val = initial_val
        self.Q_table = defaultdict(dict)
        self.C_table = defaultdict(dict)

    # updates upon full episodes
    def reward(self, reward):
         # reward the last choice
         self.choice_state_sequence[-1][2] = reward
         T = len(self.choice_state_sequence)
         G = 0
         W = 1
         for i in range(T):
             s, a, r = self.choice_state_sequence[T - i - 1] # ranges from T-1, T-2, ... 0.
             G = self.gamma*G + r
             self.C_table[s][a] = self.C(s, a) + W
             self.Q_table[s][a] = self.Q(s, a) + (W/self.C(s, a)) * (G - self.Q(s, a))
         self.choice_state_sequence = []

    def Q(self, s: str, a: str):
        try:
            return self.Q_table[s][a]
        except KeyError:
            return self.initial_val

    def C(self, s: str, a: str):
        try:
            return self.C_table[s][a]
        except KeyError:
            return self.initial_val

    def policy(self, domain, state):
        domain = list(domain)
        # Epsilon-greedy strategy
        if binomial(1, self.epsilon):
            choice = random.choice(domain)
        else:
            self.action_values = np.array([self.Q(state, a) for a in domain])
            action_idx = random.choice(np.flatnonzero(self.action_values == self.action_values.max())) # break ties randomly
            choice = domain[action_idx]
        self.choice_state_sequence.append([state, choice, 0])
        return choice
