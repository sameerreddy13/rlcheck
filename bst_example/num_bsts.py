def num_bsts(depth, domain_size):
    if depth == 0 or domain_size == 0:
        return domain_size + 1
    else:
        sum_so_far =  1
        for i in range(domain_size):
            sum_so_far += num_bsts(depth -1, i)*num_bsts(depth -1, domain_size -1 - i)
        return sum_so_far

def num_trees(depth, domain_size):
    if depth == 0 or domain_size == 0:
        return domain_size + 1
    else:
        return domain_size*num_trees(depth-1, domain_size)**2 +1
