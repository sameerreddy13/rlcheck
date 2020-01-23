class BinarySearchTree:

    def __init__(self, value):
        self.value = value
        self.left = None
        self.right = None

    def insert(self, to_add):
        if self.value > to_add:
            if self.left:
                self.left.insert(to_add)
            else:
                self.left = BinarySearchTree(to_add)
        else:
            if self.right:
                self.right.insert(to_add)
            else:
                self.right = BinarySearchTree(to_add)

    def all_values_less_than(self, value):
        if self.value >= value:
            return False
        left_less_than = True
        if self.left:
            left_less_than = self.left.all_values_less_than(value)

        right_less_than = True
        if self.right:
            right_less_than = self.right.all_values_less_than(value)
        return left_less_than and right_less_than
    
    def all_values_geq_than(self, value):
        if self.value <= value:
            return False
        left_geq_than = True
        if self.left:
            left_geq_than = self.left.all_values_geq_than(value)

        right_geq_than = True
        if self.right:
            right_geq_than = self.right.all_values_geq_than(value)
        return left_geq_than and right_geq_than

    def is_bst(self):
        left_ok = True
        if self.left:
            left_ok = self.left.all_values_less_than(self.value) and self.left.is_bst()
        right_ok = True
        if self.right:
            right_ok = self.right.all_values_geq_than(self.value) and self.right.is_bst()
        return right_ok and left_ok
    
    def __repr__(self):
        return "({} L{} R{})".format(self.value, self.left, self.right)
    
