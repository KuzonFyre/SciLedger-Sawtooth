
import pymerkletools
import sys

mt = MerkleTools(hash_type="sha256")
mt.add_leaf(sys.argv[1],True)
mt.make_tree()
if mt.is_ready:
    print(mt.get_merkle_root())
