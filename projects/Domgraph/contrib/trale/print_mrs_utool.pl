%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%   $RCSfile: print_mrs_utool.pl,v $
%%  $Revision: 1.1 $
%%      $Date: 2006/09/18 08:22:44 $
%%     Author: Stefan Mueller (Stefan.Mueller@cl.uni-bremen.de)
%%    Purpose: Communication with the scope resolver/USR display utool
%%   Language: Prolog
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

:- ensure_loaded([trale_home('chart_display/print_mrs'),
                  trale_home('chart_display/utool-interface')]).

mrs_utool(Command,FS,ErrorMessage) :-
        mrs_utool(Command,FS,_Result,ErrorMessage).

mrs_utool(Command,FS,Result,ErrorMessage) :-

    print_mrs_utool(FS,MRS,[]),
    ( get_phon(FS,Phon) ->
        utool_request(Command,Phon,'mrs-prolog',MRS,'term-prolog',Result,ErrorMessage)
    ;   utool_request(Command,'',  'mrs-prolog',MRS,'term-prolog',Result,ErrorMessage)
    ).

print_mrs_utool(FSIn,MRS,MRSRest) :-
        retractall(mynum(_)),
        assert(mynum(0)),
        
        copy_term(FSIn,FS),

        gtop_path_(TopPath),
        path_fs2pathval(TopPath,FS,TFS),
        deref(TFS,TRef,TSVs),
        inst(TRef-TSVs),
        format_to_chars('psoa(~w,',[TRef],MRS,C1),

        ind_path_(IndPath),
        path_fs2pathval(IndPath,FS,Ind),
        deref(Ind,IndRef,IndSVs),
        write_o_utool(IndRef-IndSVs,C1,C2),
        format_to_chars(',~n[',[],C2,C3),

        liszt_path_(LPath),
        path_fs2pathval(LPath,FS,Liszt),

        deref(Liszt,LisztRef,LisztSVs),
        write_rels_utool(LisztRef-LisztSVs,C3,C4),
        format_to_chars('],~n hcons([',[],C4,C5),

        hcons_path_(HPath),
        path_fs2pathval(HPath,FS,HCons),

        deref(HCons,HConsRef,HConsSVs),
        write_hcons_utool(HConsRef-HConsSVs,C5,C6),
        format_to_chars('~n ]))',[],C6,MRSRest),
	!.

print_mrs_utool(_FS,MRS,MRSRest) :-
	 format_to_chars('~N* ERROR: MRS: output failed.~n',[],MRS,MRSRest),
	 assert(error(mrs,'MRS: output failed.')).


% * Handles start with a lowercase h followed by a sequence of digits.
% * Individual variables start with a lowercase x followed by a sequence of digits.
% * Event variables start with a lowercase e followed by a sequence of digits. 

% The codec also accepts terms starting with lowercase u or i, which correspond
% to values left unspecified by the syntax-semantics interface of the grammar
% used to derive the MRS expression. These terms are ignored by the input codec.

% instantiated handle, event, index

% This is equivalent to write_o except that additional information about indexes
% is omitted.
write_o_utool(Ind-_,Chars,CharsRest) :-
        atom(Ind),
        format_to_chars('~w',[Ind],Chars,CharsRest).

% an atomic value, for instance (a_ karl) as the value of the feature named
write_o_utool(_-(a_ Val),Chars,CharsRest) :-
        format_to_chars('''~w''',[Val],Chars,CharsRest).

write_o_utool(H-SVs,Chars,CharsRest) :-
        fs2type_feats(H-SVs,handle,_),
        inst(H-handle),
        format_to_chars('~w',[H],Chars,CharsRest).

write_o_utool(IRef-ISVs,Chars,CharsRest) :-
        fs2type_feats(IRef-ISVs,Type,_Vs),
        event_or_index_type_(EIndexT),
        sub_type(EIndexT,Type),!,
        inst(IRef-ISVs),
        format_to_chars('~w',[IRef],Chars,CharsRest).


write_o_utool(IRef-ISVs,Chars,CharsRest) :-
        fs2type_feats(IRef-ISVs,Type,_Vs),
        sub_type(vtype,Type),
        format_to_chars('~w',[Type],Chars,CharsRest).


write_o_utool(_FS,Chars,Chars).



write_rels_utool(_-list,Chars,Chars) :- !.
/*
If the list is a diff-list it is always open
        format(user_error,"~N* ERROR: MRS: LISZT open.~n",[]),
        assert(error(mrs,'MRS: LISZT open.')).
*/

write_rels_utool(_-e_list,Chars,Chars) :- !.

write_rels_utool(Liszt,Chars,CharsRest) :-
        feat_fs2featval(hd,Liszt,Rel),
        write_rel_utool(Rel,Chars,C1),
        feat_fs2featval(tl,Liszt,Rels),
        ( Rels = _-e_list ->
            C2 = C1
        ;
            format_to_chars(',',[],C1,C2)
        ),
        write_rels_utool(Rels,C2,CharsRest).

write_rel_utool(RRef-RSVs,Chars,CharsRest) :-
        ( RSVs = (a_ _) -> Type = RSVs, FRs = [], Vs = []
        ; RSVs =.. [Type|Vs], approps(Type,FRs,_)
        ),
        build_keyed_feats(FRs,Vs,KeyedFeats),
        label_feat_(LabelFeat),
        select(fval(LabelFeat,H-_,handle),KeyedFeats,KeyedFeats0),
        order_features(KeyedFeats0,KeyedFeats1),

        pred_feat_(PredFeat),
        ( select(fval(PredFeat,_-(a_ Pred),_),KeyedFeats1,KeyedFeats2) -> true
        ; KeyedFeats2 = KeyedFeats1
        ),
        ( var(Pred) -> fs2type_feats(RRef-RSVs,Pred,_Vs)
        ; true
        ),
        format_to_chars('~n rel(''~w'',',[Pred],Chars,C1),
        inst(H-handle),
        format_to_chars('~w,~n     [ ',[H],C1,C2),
        write_args_utool(KeyedFeats2,C2,C3),
        format_to_chars('])',[],C3,CharsRest).

write_args_utool([],Chars,Chars).
write_args_utool([fval(F,V,_)|T],Chars,CharsRest) :-
        % utool needs exactly ARG0, RSTR and BODY,
        % so we have to do an uppercase conversion here.
        name(F,FChars),
        upper(FChars,UpperFChars),
        name(UF,UpperFChars),
        format_to_chars('attrval(''~w'',',[UF],Chars,C1),
        write_o_utool(V,C1,C2),
        format_to_chars(')',[],C2,C3),
        (T = [_|_] ->
            format_to_chars(',~n       ',[],C3,C4)
        ; C4 = C3
        ),
        write_args_utool(T,C4,CharsRest).

write_hcons_utool(_-list,Chars,Chars) :- !.
/*
If the list is a diff-list it is always open
        format(user_error,"~N* ERROR: MRS: H-CONS open.~n",[]),
        assert(error(mrs,'MRS: H-CONS open.')).
*/

write_hcons_utool(_-e_list,Chars,Chars) :- !.

write_hcons_utool(HC,Chars,CharsRest) :-
        feat_fs2featval(hd,HC,Rel),
        write_hc_utool(Rel,Chars,C1),
        feat_fs2featval(tl,HC,HCs),
        ( HCs = _-e_list ->
            C2 = C1
        ;
            format_to_chars(',',[],C1,C2)
        ),
        write_hcons_utool(HCs,C2,CharsRest).


% parameterize the features
write_hc_utool(Rel,Chars,CharsRest) :-
        sc_arg_feat_(SC_ARG),
        feat_fs2featval(SC_ARG,Rel,Sc-ScT),   inst(Sc-ScT),
        outscoped_feat_(OSFeat),
        feat_fs2featval(OSFeat,Rel,Out-OutT), inst(Out-OutT),
        fs2type_feats(Rel,Type,_),
        ( (Type = qeq; Type = geq) ->
            format_to_chars('~n ~w(~w,~w)',[Type,Sc,Out],Chars,CharsRest)
        ; format(user_error,'* ERROR: Handle constraints of type ~w are not supported by Utool',[Type]),
            fail
        ).