import random
from bst import BinarySearchTree, generate_BST

MAX_DEPTH = 4

def generate_BST(depth=0): 
    value = random.choice(range(-10, 10)) 
    tree = BinarySearchTree(value) 
    if depth < MAX_DEPTH and \
            random.choice([True, False]): 
        tree.left = generate_BST(depth+1) 
    if depth < MAX_DEPTH and \
            random.choice([True, False]): 
        tree.right = generate_BST(depth+1) 
    return tree 


def test_insert(tree, to_add):
    if not (tree.is_bst()):
        print("Not a BST {}".format(tree))
        return False
    print("BST: {}".format(tree))
    tree.insert(to_add)
    assert tree.is_bst(), "Tree: {}, Value: {}".format(tree, to_add)
    return True

if __name__ == '__main__':
    valids = 0
    for i in range(1000):
        val = test_insert(generate_BST(), random.choice(range(-20,20)))
        if val:
            valids += 1
    print(valids)
