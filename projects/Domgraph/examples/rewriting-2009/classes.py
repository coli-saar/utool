
# no_q
# all_q
# most_q

# wildcards 1 - message types, ...
W1 = [  "imp_m", 
        "prpstn_m", 
        "prop-or-ques_m", 
        "int_m", 
        "basic_imp_m", 
        "nominalization",
        "_then_a_1"     # check! 
     ]

# wildcards 2 - proper names, pronouns, numbers
W2 = [  "proper_q", 
        "pronoun_q",
        "number_q"  
     ]

# existential
E2 = [  "_some_q", 
        "_a_q", 
        "_any_q", 
        "_another_q", 
        "which_q",
        "free_relative_q", 
        "idiom_q"       # check !
     ]

# definites
D2 = [  "udef_q",
        "_the_q", 
        "def_explicit_q", 
        "def_implicit_q", 
        "_this_q_dem", 
        "_these_q_dem", 
        "_that_q_dem" 
     ]

A2 = [  "_all_q" ]

# other quantifiers
Q2 = [  "_no_q",
        "most_q",
     ]

# connectives
C2 = [  "implicit_conj",
        "_and_c",
        "_but_c",
        "_or_c" 
     ]

# modal operators
M1 = [  "_can_v_modal",
        "_could_v_modal",
        "_want_v_1", 
        "_have_v_to" 
     ]

# negation
N1 = [  "neg" ]


# other things
O1 = [  "_also_a_1", 
        "_get_v_state", 
        # "_then_a_1",  # now wildcard
        "_in_p" 
     ]

O2 = [  # "idiom_q", # now existential
        "subord",
        "_as_x_subord",
        "_in+order+to_x",
        "_if_x_then",
    # BEGIN nur in top 
        "_although_x", 
        "_because_x",
        "_before_x_h",
        "_even+though_x",
        "_if+only_x", 
        "_once_x_subord",
        "punct_prop_imp_m",
        "_so_c",
        "_so+that_x",
        "_though_x",
        "_until_x_h",
        "_when_x_subord",
        "_while_x",
        "_whilst_x"
    # END nur in top
     ]
