# Abstraction functions. 

def ngram_abstraction_fn(k):
    def ngram_abstractor(state):
        last_k = state[-k:]
        return "->".join([str(e) for e in last_k])
    return ngram_abstractor

def sequence_ngram_fn(k):
    return lambda state: ngram_abstraction_fn(k)(state)

def parent_state_ngram_fn(k, max_depth):
    return lambda state: ngram_abstraction_fn(k)(parent_state(state, max_depth))

def left_right_parent_state_ngram_fn(k, max_depth):
    return lambda state: ngram_abstraction_fn(k)(left_right_parent_state(state, max_depth))


def parent_state(state, max_depth):
    """
    >>> s = [6, True, -5, True, 0, True, 9,  True]
    >>> parent_state(s, 2)
    [6, True]
    >>> s = [2, True, 1]
    >>> parent_state(s, 2)
    [2, True, 1]
    >>> s = [2, True, 1, True, 0]
    >>> parent_state(s, 2)
    [2, True, 1]
    >>> s = [2, True, 1, True, 0, True]
    >>> parent_state(s, 2)
    [2, True, 1, True]
    >>> s = [2, True, 1, True, 0, False, True, 3, False, False]
    >>> parent_state(s, 2)
    []
    >>> s = [2, True, 1, True, 0, False, True, 3, False, True]
    >>> parent_state(s, 2)
    [2, True, 3, True]
    >>> s = [-10, True, 9, False, True, -8, True]
    >>> parent_state(s, 2)
    [-10, True]
    """
    if len(state) == 0:
        return state
    parent_trees, full_trees = get_trees_max_depth(state, max_depth)
    if len(parent_trees) == 0:
        return []
    last_tree_start = parent_trees[-1][0]
    my_posns = [last_tree_start]
    for tree_posns in reversed(parent_trees[:-1]):
        if tree_posns[1] == last_tree_start -1:
            my_posns.append(tree_posns[1])
            last_tree_start = tree_posns[0]
        elif tree_posns[2] == last_tree_start -1:
            my_posns.append(tree_posns[2])
            last_tree_start = tree_posns[0] 
        else:
            print("is this even possible??", state, parent_tres)
        my_posns.append(last_tree_start)
    if state[-1] is True:
        my_posns.append(len(state)-1)
    return [state[i] for i in range(len(state)) if i in my_posns]

def left_right_parent_state(state, max_depth):
    """
    >>> s = [6, True, -5, True, 0, True, 9,  True]
    >>> left_right_parent_state(s, 2)
    [6, 'RIGHT']
    >>> s = [2, True, 1]
    >>> left_right_parent_state(s, 2)
    [2, 'LEFT', 1]
    >>> s = [2, True, 1, True, 0]
    >>> left_right_parent_state(s, 2)
    [2, 'LEFT', 1]
    >>> s = [2, True, 1, True, 0, True]
    >>> left_right_parent_state(s, 2)
    [2, 'LEFT', 1, 'RIGHT']
    >>> s = [2, True, 1, True, 0, False, True, 3, False, False]
    >>> left_right_parent_state(s, 2)
    []
    >>> s = [2, True, 1, True, 0, False, True, 3, False, True]
    >>> left_right_parent_state(s, 2)
    [2, 'RIGHT', 3, 'RIGHT']
    >>> s = [-10, True, 9, False, True, -8, True]
    >>> left_right_parent_state(s, 2)
    [-10, 'RIGHT']
    """
    state = state[:]
    parent_trees, full_trees = get_trees_max_depth(state, max_depth)
    if len(parent_trees) == 0:
        return []
    last_tree_start = parent_trees[-1][0]
    my_posns = [last_tree_start]
    for tree_posns in reversed(parent_trees[:-1]):
        if tree_posns[1] == last_tree_start -1:
            my_posns.append(tree_posns[1])
            state[tree_posns[1]]= "LEFT"
            last_tree_start = tree_posns[0]
        elif tree_posns[2] == last_tree_start -1:
            my_posns.append(tree_posns[2])
            state[tree_posns[2]]= "RIGHT"
            last_tree_start = tree_posns[0] 
        else:
            print(tree_posns, last_tree_start)
            print("is this even possible??", state, parent_trees)
        my_posns.append(last_tree_start)
    if state[-1] is True:
        last_posn = len(state) - 1
        if last_posn == parent_trees[-1][1]:
            state[last_posn] = 'LEFT'
        elif last_posn == parent_trees[-1][2]:
            state[last_posn] = 'RIGHT'
        else:
            print("is this even possible?? (2)", state, parent_trees[-1], parent_trees, full_trees)
        my_posns.append(len(state)-1)
    return [state[i] for i in range(len(state)) if i in my_posns]

def get_trees_max_depth(state, max_depth):
    """
    >>> s = [6, True, -5, True, 0, True, 9,  True]
    >>> get_trees_max_depth(s, 2)
    ([(0, 1, 7)], [(4, False, False), (6, False, False), (2, 3, 5)])
    >>> s = [2, True, 1]
    >>> get_trees_max_depth(s, 2)
    ([(0, 1, -1), (2, -1, -1)], [])
    >>> s = [2, True, 1, True, 0]
    >>> get_trees_max_depth(s, 2)
    ([(0, 1, -1), (2, 3, -1)], [(4, False, False)])
    >>> s = [2, True, 1, True, 0, False, True, 3, False, False]
    >>> get_trees_max_depth(s, 2)
    ([], [(4, False, False), (2, 3, 5), (7, 8, 9), (0, 1, 6)])
    >>> s = [2, True, 1, True, 0, False, True, 3, False, True]
    >>> get_trees_max_depth(s, 2)
    ([(0, 1, 6), (7, 8, 9)], [(4, False, False), (2, 3, 5)])
    >>> get_trees_max_depth([-10, True, 9, False, True, -8, True], 2)
    ([(0, 1, 6)], [(5, False, False), (2, 3, 4)])
    """
    parent_stack = []
    full_trees = []
    
    # Utility function which puts parents in full_trees if all their children are in full_trees
    def pop_finished_parents():
        for i in reversed(range(len(parent_stack))):
            positions = parent_stack[i]
            if -1 in positions:
                break
            children_positions = [pos[0] for pos in full_trees]
            left_child_pos = positions[1] + 1
            right_child_pos = positions[2] + 1
            if state[positions[1]] and left_child_pos not in children_positions:
                break
            if state[positions[2]] and right_child_pos not in children_positions:
                break
            full_trees.append(positions)
            parent_stack.pop(i)
    
    for i in range(len(state)):
        e = state[i]
        if 'int' in str(type(e)):
            if len(parent_stack) < max_depth:
                parent_stack.append((i, -1, -1))
            else:
                full_trees.append((i, False, False))
                pop_finished_parents()
        else:
            parent = parent_stack[-1]
            if parent[1] == -1:
                parent_stack[-1] = (parent[0], i, -1)
            elif parent[2] == -1:
                parent_stack[-1] = (parent[0], parent[1], i)
                if not e:
                    full_trees.append(parent_stack.pop())
                    pop_finished_parents()
    parent_stack = [posns for posns in parent_stack if posns not in full_trees]
    return  parent_stack , full_trees
