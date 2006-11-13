%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%   $RCSfile: chart_display.pl,v $
%%  $Revision: 1.29 $
%%      $Date: 2006/09/18 08:22:44 $
%%     Author: Stefan Mueller (Stefan.Mueller@cl.uni-bremen.de)
%%    Purpose: Communication with the TCL/TK chart display.
%%   Language: Prolog
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% $Log: chart_display.pl,v $
%% Revision 1.29  2006/09/18 08:22:44  stefan
%% integrated utool
%%
%% Revision 1.28  2005/06/19 17:21:10  stefan
%% *** empty log message ***
%%
%% Revision 1.27  2005/04/22 15:13:12  stefan
%% *** empty log message ***
%%
%% Revision 1.26  2005/03/31 13:26:18  stefan
%% played around with the daughters of rules, should be integrated into the mother
%% feature structure before printing
%%
%% Revision 1.25  2003/12/02 20:53:17  stefan
%% trace vergessen, jetzt wieder so wie vorher
%%
%% Revision 1.24  2003/12/02 20:52:41  stefan
%% nichts geändert
%%
%% Revision 1.23  2003/11/04 19:19:16  stefan
%% fixed the prompt functionality in the interactive chart diaplay mode
%%
%% Revision 1.22  2003/10/15 18:48:40  stefan
%% *** empty log message ***
%%
%% Revision 1.21  2003/10/15 18:34:34  stefan
%% I now also call the tokanizer for reading lines. This eliminates read.pl.
%%
%% Revision 1.20  2003/10/08 20:42:52  stefan
%% added soft exception handling for the interactive level
%%
%% Revision 1.19  2003/10/07 12:33:49  stefan
%% fixed init_chart_display, when the chart display was used for the first
%% time some edges were missing
%%
%% Revision 1.18  2003/08/11 08:28:01  dm
%% no change this time, just forgot to add comment on what I did for the
%% last check in: Added resetting of num/1 to init_chart_display which is
%% needed to let chart edges start with an id that is (number of rules +
%% 1) so that the same index can be used for the chart display. (By
%% moving this here, less needs to be redefined in ale_redefinitions.pl)
%%
%% Revision 1.17  2003/08/11 08:24:26  dm
%% *** empty log message ***
%%
%% Revision 1.16  2003/07/11 06:36:15  stefan
%% Hm, seem to have forgotten to check some of this in.
%% Sorry
%%
%% Revision 1.15  2003/05/27 18:03:45  stefan
%% - Made the chart display more robust. If somebody closes the TCL/TK window
%%   or kills the process, a new window is opened.
%%
%% - Generation from chart edges now works.
%%
%% Revision 1.14  2003/05/27 03:09:18  dm
%% Changed lex/3 to lex/2 since that's the way it's now defined in ale.pl
%%
%% Revision 1.13  2003/05/25 22:07:20  stefan
%% After a unification failure it is tested if a real unification would
%% lead to an infinite loop in unblocked constraints. This can happen
%% if the deblocking of constraints appears before a unification failure.
%%
%% Revision 1.12  2003/05/19 18:51:22  stefan
%% changed a call_residue (was to much)
%%
%% Revision 1.11  2003/05/19 15:34:27  stefan
%% - Added printing of active edge
%% - Added postponing of residues. Residues of the active edge
%%   and the passive edge are ignored during stepwise unification.
%%   If the unification does not succeed the failure passes are shown.
%%   If it does succeed the blocked goals are applied.
%% - Moved tests for memory overflow into rec/6. It is done
%%   via exception handling which is more genral than testing a fixed
%%   number.
%%
%% Revision 1.10  2003/05/16 03:41:13  dm
%% - Added test for existence of TCL code to chart_display/0.
%% - Removed :- chart_display. from code, so it now needs to be called
%%   explicitly for switching on the chart display (needed since
%%   startup.pl now always load the code in chart_display.pl)
%%
%% Revision 1.9  2003/05/13 09:08:49  stefan
%% put MRS printing in a different file
%%
%% Revision 1.8  2003/05/10 13:48:53  stefan
%% *** empty log message ***
%%
%% Revision 1.7  2003/05/09 16:12:20  stefan
%% Now interactive debugging, application of rules to edges in the chart.
%%
%% Revision 1.6  2003/05/09 09:21:00  stefan
%% *** empty log message ***
%%
%% Revision 1.5  2003/05/03 10:03:58  stefan
%% integrating again
%%
%% Revision 1.4  2003/05/02 20:35:00  dm
%% simplified code
%%
%% Revision 1.3  2003/05/02 02:41:01  dm
%% made english the default
%%
%% Revision 1.2  2003/05/02 02:40:26  dm
%% Made paths used as arguments of "set loadpath" and "source" in the tcl
%% code relative to the trale_home search path. Minor code
%% simplification.
%%
%% Revision 1.1  2003/05/02 02:09:43  dm
%% Added Stefan's chart display.
%%
%% Revision 1.3  2003/04/28 09:19:43  stefan
%% completed changes with direct access to ale edges
%%
%% added better registration for rules. Now disjunctions in rule
%% specifications do not cause appearance of two rules at the left
%% hand side of the display. Better reflects the internal representation
%% and necessary for the chart debugger.
%%
%% Revision 1.2  2003/04/27 21:49:43  stefan
%% before touching the parser code/drawing during parse
%%
%% Revision 1.2  2003/04/08 08:47:14  stefan
%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


:- use_module(library(system)).
:- use_module(library(charsio)). % format_to_chars
:- use_module(library(sockets)). % for checking whether there's input at stdin
:- use_module(library(lists),[select/3]).
:- use_module(library(timeout)).

:- ensure_loaded([trale_home('chart_display/sic'), % timer, sformat
                  trale_home('chart_display/ale_redefinitions'),
                  trale_home('chart_display/statistics'),
                  trale_home('chart_display/print_mrs'),
                  trale_home('chart_display/print_mrs_utool'),
                  trale_home('chart_display/utool-interface')]).


% to close stuff if we reload this file
% otherwise we get invalid streams since they are newly declared
% there would remain an unused chart display.
:- ( current_predicate(shutdown_wish/0) -> shutdown_wish
   ; true
   ).

lc :- [trale_home('chart_display/chart_display')].


% draw_solutions
% draw_direct_daughters(Id)

% draw_rules

:- dynamic to_wish_stream/1.
:- dynamic from_wish_stream/1.
:- dynamic daughter/1.          % grammar debugging
:- dynamic marked/2.
:- dynamic selected_rule/1.     % grammar debugging
:- dynamic printed_item/1.      % last output
:- dynamic active_edge/4.
:- dynamic chart_ale_id/2.
:- dynamic fs/1.
:- dynamic chart_display/1.
:- dynamic tcl_warnings/1.
:- dynamic language/1.
:- dynamic hacky_rule_printing/1,chart_debug/1,discontinuous_edges/1.


% All edges in the chart have a unique number.
% Numbers have to be continuous.
% Rules are entered first. There numbers are determined
% according to the counter rule_counter. Chart edges
% that correspond to ALE edges have higher numbers.

% damit man den Code nicht laden muss
init_chart_display_if_on :-
  ( chart_display(on)
  -> init_chart_display
  ;  retractall(rule_counter(_)),
     assert(rule_counter(-1)) ).

finish_chart_display_if_on :-
   ( chart_display(on) ->
   %   draw_rules,
       show_bottom
   %   view_compact
   ; true ).

% -------------------------------------------------------

chart_display   :-
   retractall(chart_display(_)),
   (  absolute_file_name(trale_home('chart_display/TCL/chart/chart.tcl'),
			_,
			[access(read),file_errors(fail)])
   -> assert(chart_display(on))
   ;  format(user_error,'~N~n**Error: The chart display is not part of your distribution.~n~n',[]),
      format(user_error,'         The tcl/tk tool that displays the chart was developed at the DFKI~n',[]),
      format(user_error,'         and they ask you to sign a (free) license.~n',[]),
      format(user_error,'         Contact trale-support@ling.osu.edu for the details.~n',[]),
      assert(chart_display(off))
   ).
   
nochart_display :- retractall(chart_display(_)),assert(chart_display(off)).


fs   :- retractall(fs(_)),assert(fs(on)).
nofs :- retractall(fs(_)),assert(fs(off)).

:- fs.

mrs   :- retractall(mrs(_)),assert(mrs(on)).
nomrs :- retractall(mrs(_)),assert(mrs(off)).

:- nomrs.


display_mrs   :- retractall(display_mrs(_)),assert(display_mrs(on)).
nodisplay_mrs :- retractall(display_mrs(_)),assert(display_mrs(off)).

:- nodisplay_mrs.

scope_mrs   :- retractall(scope_mrs(_)),assert(scope_mrs(on)).
noscope_mrs :- retractall(scope_mrs(_)),assert(scope_mrs(off)).

:- noscope_mrs.

debug_mrs   :- retractall(debug_mrs(_)),assert(debug_mrs(on)).
nodebug_mrs :- retractall(debug_mrs(_)),assert(debug_mrs(off)).

:- nodebug_mrs.


german  :- retractall(language(_)),assert(language(german)).
english :- retractall(language(_)),assert(language(english)).

:- english.

tcl_warnings   :- retractall(tcl_warnings(_)),assert(tcl_warnings(on)).
notcl_warnings :- retractall(tcl_warnings(_)),assert(tcl_warnings(off)).

:- tcl_warnings.

hrp   :- retractall(hacky_rule_printing(_)),assert(hacky_rule_printing(on)).
nohrp :- retractall(hacky_rule_printing(_)),assert(hacky_rule_printing(off)).

:- nohrp.

% this is a flag that can be used by the user in the constraints
% to conditionalize some debugging output

% Example:
%
% undelayed_subj_verb_agreement([El|T1],Per,Num) if
%  prolog((chart_debug(on) ->
%          write(user_error,'Trying to assign accusative.'),
%          nl(user_error)
%         ;true
%         )),
%  assign_acc(El),
%  subj_verb_agreement(T1,Per,Num).

chart_debug   :- retractall(chart_debug(_)),assert(chart_debug(on)).
nochart_debug :- retractall(chart_debug(_)),assert(chart_debug(off)).

:- nochart_debug.

discontinuous_edges   :- retractall(discontinuous_edges(_)),assert(discontinuous_edges(on)).
nodiscontinuous_edges :- retractall(discontinuous_edges(_)),assert(discontinuous_edges(off)).

:- nodiscontinuous_edges.

timeout_time_(T) :-
   ( current_predicate(timeout_time/1) ->
      timeout_time(T)
   ;   %% The default if the user hasn't defined timeout_time/1
      T = 2000
   ).

% -------------------------------------------------------

get_to_wish_stream(TO_WISH_S) :-
  chart_display(on),
  (  to_wish_stream(TO_WISH_S)
  -> true
  ;  open_wish_and_register_rules(TO_WISH_S)
  ).

% I consider it a bug that pipe cannot be supplied with
% an option to change the character encoding.
open_wish_and_register_rules(TO_WISH_S) :-
  exec(wish,[pipe(TO_WISH_S),pipe(FROM_WISH_S),null],_Pid),
  asserta(to_wish_stream(TO_WISH_S)),
  asserta(from_wish_stream(FROM_WISH_S)),
    
  format_wish(TO_WISH_S, 'wm withdraw .~n',[]),

  absolute_file_name(trale_home('chart_display/TCL'),LoadPath),
  format_wish(TO_WISH_S, 'set loadpath ~w~n',[LoadPath]),

%  format_wish(TO_WISH_S, 'set chart(verbose) 1~n',[]),    % for debugging only

  absolute_file_name(trale_home('chart_display/TCL/chart/chart.tcl'),ChartTcl),
  format_wish(TO_WISH_S, 'source ~w~n',[ChartTcl]),

  format_wish(TO_WISH_S, 'set chart(prolog) 1~n',[]),
  (  language(german)
  -> format_wish(TO_WISH_S, 'set chart(english) 0~n',[])
  ;  true ),

  format_wish(TO_WISH_S, 'set chart(popup_activepassive) 0~n',[]),
  format_wish(TO_WISH_S, 'set chart(popup_leftright) 0~n',[]),
  format_wish(TO_WISH_S, 'set chart(popup_view) 0~n',[]),
%  format_wish(TO_WISH_S, 'set chart(popup_style) 0~n',[]),
%  format_wish(TO_WISH_S, 'set chart(popup_playback) 0~n',[]),
  format_wish(TO_WISH_S, 'set chart(iconname) icon.xbm~n',[]),
  format_wish(TO_WISH_S, 'set iconname_name "Ch: Trale"~n',[]).


% this sleeping stuff is due to some communication problem
% under SUSE Linux 8.0-8.2 some caracters seem to get lost
sleep_time :-
   sleep(0.001).

format_wish(Stream,String,Args) :-
   format(Stream,String,Args), 
   sleep_time.

% eigentlich muss bei parser_quit der Stream geloescht werden
init_chart_display :-
  (  get_to_wish_stream(TO_WISH_S)
  -> catch(  format_wish(TO_WISH_S, 'init_chart_display 0 Trale~n',[]),
             _Catcher,
              ( write_error('\nDisplay wird neu geoeffnet.\n\n'),
                reset_wish,
                open_wish_and_register_rules(NEW_TO_WISH_S),
                assert(to_wish_stream(NEW_TO_WISH_S))
              )
           ),
      retractall(marked(_,_)),
     retractall(daughter(_)),

     % wenn Display mit Quit geschlossen wurde, muss das erneut
     % gesendet werden.
     define_actions,
     disable_debug_actions,
      register_rules,                 % this has to be done everytime since the grammar may be reloaded in the meantime
     draw_rules
  ;  true ),

  % for the chart display we have the first N numbers
  % for chart items that correspond to grammar rules:
  retractall(num(__)),rule_counter(RC), FirstId is RC + 1, assert(num(FirstId)).

reset_chart_display :-
        retractall(to_wish_stream(_)),
        retractall(from_wish_stream(_)),
        init_chart_display.

define_actions :-
  (language(german) ->
   define_parser_action('Ausgabe',                             1, edge)
  ;define_parser_action('Output',                              1, edge)
  ),
  
  (language(german) ->
   define_parser_action('"Zeige direkte Töchter"',             2, edge)
  ;define_parser_action('"Show immediate daughters"',          2, edge)
  ),
  
  (language(german) ->
   define_parser_action('"Zeige Regel"',                       3, rule)
  ;define_parser_action('"Show rule"'  ,                       3, rule)
  ),

  (language(german) ->
   define_parser_action('"Zeige Ergebnisse dieser Regel"',     4, rule)
  ;define_parser_action('"Show results involving this rule"',  4, rule)
  ),   

  (language(german) ->
   define_parser_action('"Wähle Tochter"',                     5, edge)
  ;define_parser_action('"Select daughter"',                   5, edge)
  ),

  (language(german) ->
   define_parser_action('"Wähle Regel"',                       6, rule)
  ;define_parser_action('"Select rule"',                       6, rule)
  ),

  (language(german) ->
   define_parser_action('"Nur Ergebnisse"',                    7, main)
  ;
   define_parser_action('"Results only"',                      7, main)
  ),
  
  (language(german) ->
   define_parser_action('"Zeige Residuum"',                    8, main)
  ;
   define_parser_action('"Show Residue"',                      8, main)
  ),
  
  (language(german) ->
   define_parser_action('"Zeige Residuum nicht"',              9, main)
  ;
   define_parser_action('"Hide Residue"',                      9, main)
  ),
  (language(german) ->
   define_parser_action('"Zeige aktive Kante"',               10, main)
  ;
   define_parser_action('"Show active edge"',                 10, main)
  ),
  (language(german) ->
   define_parser_action('"Generiere"',                        11, edge)
  ;
   define_parser_action('"Generate"',                         11, edge)
  ),
  (language(german) ->
   define_parser_action('"Zeige MRS"',                        12, edge)
  ;
   define_parser_action('"Show MRS"',                         12, edge)
  ),
  (language(german) ->
   define_parser_action('"Skope MRS"',                        13, edge)
  ;
   define_parser_action('"Scope MRS"',                        13, edge)
  ).

  
%  define_parser_action('"Nur komprimierte Ergebnisse"',       9, main),
%  define_parser_action('"Nur kontinuierliche Kanten"',       10, main),

%  define_parser_action('"Nur komprimierte Kanten"',          11, main),

%  define_parser_action('"Zeige aktive Kante"',               12, main),

%  define_parser_action('"Ausgabe mit  Domänen"',             13, main),
%  define_parser_action('"Ausgabe ohne Domänen"',             14, main).

disable_debug_actions :- 
   disable_menu_entry(5),     % select daughter
   disable_show_active_edge.     

/*
disable_debug_actions :-
   disable_menu_entry(2),       % disabled since not implemented yet
   disable_menu_entry(4),       % disabled since not implemented yet
   disable_menu_entry(5),       % disabled since not implemented yet
%   disable_menu_entry(7),       % disabled since not implemented yet
  disable_menu_entry(6).
*/

enable_show_active_edge :-
   enable_menu_entry(10).

disable_show_active_edge :-
   disable_menu_entry(10).

enable_select_rule :-
   enable_menu_entry(6).

enable_select_daughter :-
   enable_menu_entry(5).

draw_item(Id,Start,End,RuleName,Daughters) :-
  (  get_to_wish_stream(TO_WISH_S)
  -> escape_blanks(RuleName,EscRuleName),
     map_dtrs(Daughters,DisplayDaughters),
    
    % draw_edge chartid edgeid start end label status extension children
    format_wish(TO_WISH_S, 'draw_edge 0 ~w ~w ~w ~w 0 0 {',[Id,Start,End,EscRuleName]), 
    write_d(DisplayDaughters,TO_WISH_S),
    format_wish(TO_WISH_S, ' }~n',[])
  ; true ).

escape_blanks(RuleName,EscRuleName) :-
  name(RuleName,RuleNameS),
  escape_blanks_(RuleNameS,EscRuleNameS),
  name(EscRuleName,EscRuleNameS).

escape_blanks_([],[]).
escape_blanks_([32|T],[92,32|ET]) :- !,
  escape_blanks_(T,ET).
escape_blanks_([H|T],[H|ET]) :-
  escape_blanks_(T,ET).

write_d([],_).
write_d([H|T],Stream) :-
  write(Stream,H),
  sleep_time,
  (  T == []
  -> true
  ;  write(Stream,' '),
     sleep_time,
     write_d(T,Stream)).

clear_chart_view :-
  (  to_wish_stream(TO_WISH_S)
  -> format_wish(TO_WISH_S, 'clear_view 0~n',[])
  ;  true ).

define_parser_action(Name,Id,EdgeOrMain) :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S,'def_action 0 ~w ~w ~w~n',[Name,Id,EdgeOrMain]).

% scrolls back to the bottom of the display.
show_bottom :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'show_bottom 0~n',[]).

mark_edge(EdgeId,Color) :-
  retractall(marked(EdgeId,_)),
  assert(marked(EdgeId,Color)),
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'mark_edge 0 ~w ~w~n',[EdgeId,Color]).

unmark_edge(EdgeId) :-
  retractall(marked(EdgeId,_)),
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'unmark_edge 0 ~w~n',[EdgeId]).

% this and the recording of the marked edges is needed
% because of a bug in the TCL/TK code which I am unable to fix.
% If one displays a different subset of edges in the chart, the
% markings are lost.
% Unfortunately this only fixes the bug partly since it
% does not influence the drawings of all edges. This is done by the
% display without interaction with prolog.
remark_edges :-
        to_wish_stream(TO_WISH_S),
        ( marked(EdgeId,Color),
            format_wish(TO_WISH_S, 'mark_edge 0 ~w ~w~n',[EdgeId,Color]),
            fail
        ; true
        ).

mark_rule(EdgeId,Color) :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'mark_rule 0 ~w ~w~n',[EdgeId,Color]).

unmark_rule(EdgeId) :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'unmark_rule 0 ~w~n',[EdgeId]).

enable_menu_entry(ActionId) :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'enable_menu_entry 0 ~w~n',[ActionId]).

disable_menu_entry(ActionId) :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'disable_menu_entry 0 ~w~n',[ActionId]).

close_chart_display :-
  (  to_wish_stream(TO_WISH_S)
  -> catch( format_wish(TO_WISH_S,'quit_parser 0~n',[]),
           _Catcher,
           reset_wish
          )
  ;  true ).

shutdown_wish :-
  %close_chart_display,
  (  to_wish_stream(TO_WISH_S)
  -> catch( format_wish(TO_WISH_S,'exit~n',[]),
           _Catcher,
           true
          )
  ; true
  ),
  reset_wish.

reset_wish :-
  (from_wish_stream(FW) -> close(FW)
  ;true),
  (to_wish_stream(TW) -> close(TW)
  ; true),
  retractall(from_wish_stream(_)),
  retractall(to_wish_stream(_)).

%--------------------------------------------------------------
%
% Actions
%
%--------------------------------------------------------------
% Edge Actions
%--------------------------------------------------------------

% Zeichen ausgeben
do_edge_action(_ChartId,1,EdgeId) :-
   unselect_printed_item,
   assert(printed_item(EdgeId)),
   mark_edge(EdgeId,blue),
%  write(user_error, EdgeId),nl(user_error),

   print_fs_id(EdgeId).


% Zeichne direkte Töchter
do_edge_action(_ChartId,2,EdgeId) :-   
   clear_chart_view,
   retractall(drawn(_)),
   draw_rules,
   edge(EdgeId,Start,End,_Tag,_SVs,Daughters,RuleName),
   draw_id_list(Daughters),
   draw_item(EdgeId,Start,End,RuleName,Daughters),
   remark_edges.

% Regel ausgeben
do_edge_action(_ChartId,3,EdgeId) :-   
   unselect_printed_item,
   assert(printed_item(EdgeId)),
   mark_rule(EdgeId,blue),
   sm_rule(EdgeId,_RuleName,Reference),

%   write(user_error,RuleName),nl(user_error),
   
   print_rule(Reference). % calls the ALE predicate
   

% all edges that are the result of the application of the rule with Id RuleId
do_edge_action(_ChartId,4,RuleId) :-
   clear_chart_view,
   retractall(drawn(_)),
   draw_rules,

   sm_rule(RuleId,RuleName,_),
   foreach( call_residue(clause(edge(Index,Start,End,_Tag,_SVs,Dtrs,RuleName),true),_Residue),
            %format('~w ~w ~w ~w ~w~n',[SignId,Start,End,RuleName,Daughters]),
            draw_item(Index,Start,End,RuleName,Dtrs)
          ),
   remark_edges.

% select the next daughter
do_edge_action(_ChartId,5,EdgeId) :-
  mark_edge(EdgeId,red),
  test_active_rule_and_recent_daughter(EdgeId),
  asserta(daughter(EdgeId)).

% select rule
% unselects all previously selected daughters and rule
do_edge_action(_ChartId,6,RuleId) :-
   unselect_rule,
   unselect_daughters,
   disable_show_active_edge,
   retractall(active_edge(_,_,_,_)),
   assert(selected_rule(RuleId)),
   mark_rule(RuleId,green),
   enable_select_daughter.

% has to be rewritten to get the path to semantics from sem1, where
% it is specified anyway.
do_edge_action(_ChartId,11,EdgeId) :-
    ( (current_predicate(generate/4) % Trale's generator
      ;current_predicate(g/1)        % Aurelien's generator
      ) ->
        gen_pathes_(GenPath),
        edge(EdgeId,_Left,_Right,Ref,SVs,_Dtrs,_RuleName),
        ( make_generation_desc(GenPath,Ref,SVs,Desc)  ->
            format(user_error,'~nGenerating ...~n',[]),
            format(user_error,'~n~w~n',[Desc]),
            ( current_predicate(g/1) -> wrapped_g(Desc)
            ;                           wrapped_gen(Desc)
            )
        ; write_warning('gen_pathes/1 wrongly specified?','Ist gen_pathes/1 falsch angegeben?')
        )
    ; write_warning('The grammar is not prepared for generation. Please type "parse_and_gen." and reload the grammar.',
                    'Die Grammatik ist nicht zum Generieren vorbereitet. Geben Sie "parse_and_gen." ein, und laden Sie die Grammatik erneut.')
    ).

% display the MRS
do_edge_action(_ChartId,12,EdgeId) :-
    edge(EdgeId,_Left,_Right,Ref,SVs,_Dtrs,_RuleName),
    mrs_utool(display,Ref-SVs,ErrorMessage),
    ( (debug_mrs(on),ErrorMessage \== "") ->
        format(user_error,"~N* ERROR: ~s~n",[ErrorMessage])
    ; true
    ).

% scope the MRS
do_edge_action(_ChartId,13,EdgeId) :-
    edge(EdgeId,_Left,_Right,Ref,SVs,_Dtrs,_RuleName),
    nl,nl,
    mrs_utool(solve,Ref-SVs,_Scopings,ErrorMessage),
    ( (debug_mrs(on),ErrorMessage \== "") ->
        format(user_error,"~N* ERROR: ~s~n",[ErrorMessage])
    ; true
    ).



do_edge_action(_ChartId,ActionNumber,EdgeId) :-
  action_warning(ActionNumber,EdgeId).

%--------------------------------------------------------------
% Main Actions
%--------------------------------------------------------------

% Ausgabe aller Lösungen
do_main_action(_ChartId,7) :-
  draw_solutions,
  remark_edges.

do_main_action(_ChartId,8) :-
  show_residue.

do_main_action(_ChartId,9) :-
  hide_residue.

do_main_action(_ChartId,10) :-
  print_active_edge.

do_main_action(_ChartId,ActionNumber) :-
  action_warning(ActionNumber,none).



% Regel ausgeben
do_rule_action(_ChartId,4,EdgeId) :-
  sm_rule(EdgeId,_RuleName,Reference),
  print_rule(Reference).

do_rule_action(_ChartId,ActionNumber,EdgeId) :-
  action_warning(ActionNumber,EdgeId).

%action_warning(ActionNumber,EdgeId)
action_warning(ActionNumber,none) :-
  sformat(EWarning,'Action failed for event ~w!~n~n',[ActionNumber]),
  sformat(GWarning,'Für Ereignis ~w ist die Aktion fehlgeschlagen!~n~n',[ActionNumber]),
  write_warning(EWarning,GWarning).

action_warning(ActionNumber,EdgeId) :-
  sformat(EWarning,'Action failed for event ~w and edge ~w!~n~n',[ActionNumber,EdgeId]),
  sformat(GWarning,'Für Ereignis ~w und Kante ~w ist die Aktion fehlgeschlagen!~n~n',[ActionNumber,EdgeId]),
  write_warning(EWarning,GWarning).


unselect_daughters :-
  retract(daughter(EdgeId)),
  unmark_edge(EdgeId),
  fail.

unselect_daughters. 

unselect_rule :-
  retract(selected_rule(EdgeId)),
  unmark_rule(EdgeId),
  retract(selected_rule(_)).

% Es gab keine
unselect_rule. 

unselect_printed_item :-
  retract(printed_item(EdgeId)),
  ( sm_rule(EdgeId,_,_),
    unmark_rule(EdgeId)
  ;
    unmark_edge(EdgeId)
  ),
  retractall(printed_item(_)).

% Es gab keine
unselect_printed_item. 

unselect_all_items :-
  unselect_daughters,
  unselect_rule,
  unselect_printed_item.


% gekapselt, damit Ausgabe auf TCL/TK oder Terminal erfolgen kann.
write_warning(English,German) :-
  tcl_warnings(on),!,
  to_wish_stream(TO_WISH_S),
  
  ( language(german) ->
      format_wish(TO_WISH_S, 'modal_dialog "Chart-Display: Trale" "~w" "Ok"~n', [German])
  ; otherwise ->
      format_wish(TO_WISH_S, 'modal_dialog "Chart-Display: Trale" "~w" "Ok"~n', [English])
  ).
  

write_warning(English,German) :-
  nl(user_error),nl(user_error),
  ( language(german) ->
      write(user_error,'Warnung: '),
      write(user_error,German)
  ; otherwise        ->
      write(user_error,'Warning: '),
      write(user_error,English)
  ),
  nl(user_error),nl(user_error),prompt.



:- dynamic drawn/1.

draw_solutions :-
   clear_chart_view,

   retractall(drawn(_)),
   draw_rules,

   foreach( solution(Id),
            draw_chart_edge_and_dtrs(Id)
   ).

%-----------------
        

draw_chart_edge_and_dtrs(Id) :-
  drawn(Id).        % do not draw things twice
                    % daughters may have multiple mothers (strange eh?)

draw_chart_edge_and_dtrs(EdgeId) :-
  assert(drawn(EdgeId)),

%  write(EdgeId),nl,
  ( EdgeId = empty(EId,L),
    empty_cat(EId,_N,_Tag,_SVs,Dtrs,RuleName),
    chart_ale_id(DisplayId,EdgeId),
    Left = L,
    Right = L
  ; edge(EdgeId,Left,Right,_TagOut,_SVsOut,Dtrs,RuleName),
    DisplayId = EdgeId
  ),

  draw_daughters(Dtrs),

  draw_item(DisplayId,Left,Right,RuleName,Dtrs).


draw_daughters([]).

draw_daughters([H|T]) :-
  draw_chart_edge_and_dtrs(H),
  draw_daughters(T).

% called by immediate daughters
draw_chart_edge(EdgeId) :-
  ( EdgeId = empty(EId,L),
    empty_cat(EId,_N,_Tag,_SVs,Dtrs,RuleName),
    chart_ale_id(DisplayId,EdgeId),
    Left = L,
    Right = L
  ; edge(EdgeId,Left,Right,_TagOut,_SVsOut,Dtrs,RuleName),
    DisplayId = EdgeId
  ),

  draw_item(DisplayId,Left,Right,RuleName,Dtrs).

draw_id_list([]).
draw_id_list([EdgeId|Ids]) :-
        draw_chart_edge(EdgeId),
        draw_id_list(Ids).


%--------------------

% the following corresponds to rule/1 from ale.pl
% in comparison to rule/1 the rule to be printed is identified
% by reference to Reference of the clause.

print_rule(Reference) :-
          call_residue(clause(alec_rule(RuleName,DtrsDesc,_,Moth,_,_),true,Reference),_Residue),
%  (RuleName rule Moth ===> DtrsDesc),
  empty_assoc(AssocIn),  
  call_residue((satisfy_dtrs(DtrsDesc,DtrCats,[],Dtrs,gdone),
                add_to(Moth,TagMoth,bot),
                CatsOut = [TagMoth-bot|DtrCats],
                extensionalise_list(CatsOut)),Residue),
  \+ \+ (((current_predicate(portray_rule,portray_rule(_,_,_,_)),
           portray_rule(TagMoth,bot,Dtrs,Residue)) -> true
         ;(current_predicate(hacky_portray_rule,hacky_portray_rule(_,_,_)),
           hacky_rule_printing(on),
           hacky_portray_rule(RuleName,TagMoth,Dtrs,Residue)) -> fail % please fail here and do not ask
         ;
           nl, write('RULE: '), write(RuleName),
           build_iqs(Residue,Iqs,FSResidue),
           (show_res -> residue_args(FSResidue,ResArgs,CatsOut) ; ResArgs = CatsOut),
           duplicates_list(ResArgs,AssocIn,DupsMid,AssocIn,VisMid,0,NumMid),
           duplicates_iqs(Iqs,DupsMid,DupsMid2,VisMid,_,NumMid,_),
           nl, nl, write('MOTHER: '), nl,
           nl, tab(2), pp_fs(TagMoth,bot,DupsMid2,DupsMid3,AssocIn,VisMid2,2,AssocIn,HDMid),
           nl, nl, write('DAUGHTERS/GOALS: '),   
           show_rule_dtrs(Dtrs,DupsMid3,DupsMid4,VisMid2,VisMid3,HDMid,HDMid2),
           nl,nl, tab(2), pp_iqs(Iqs,DupsMid4,DupsOut,VisMid3,VisOut,2,HDMid2,HDOut),
           ((show_res,FSResidue \== [])
           -> nl, nl, write('Residue:'), pp_residue(FSResidue,DupsOut,_,VisOut,_,HDOut,_)
           ; true), nl
         ),
         query_proceed).


print_rule(_) :- % please do not fail!
  nl,prompt.      


% this only works if one has the dtrs in the structure
hacky_portray_rule(RuleName,Tag-SVs,_Dtrs,Residue) :-
   grisu_flag,			% fails immediately if grisu not on
   set_title(rule:RuleName),
   grisu_pp_fs_res(Tag,SVs,Residue,_),
   clear_title.

/*

Something should be done to the daughters.

%           writeq(Reference),nl,
           trace,
           dtrs_to_desc_list(Dtrs,DtrsWithoutCat),
           grisu_pp_fs_res(_Tag,DtrsWithoutCat,Residue,_).
           /*
           DtrsWithoutCat = [First|_],
           add_to(dtrs:ne_list,TagMoth,bot),
           */



/*
print_rule(RuleName) :-
  sformat(Warning,'Es gibt keine Regel Names "~w"!~n',[RuleName]),
  write_warning(Warning).
*/


% we are looking for an edge that borders the active edge (End)
% and that matches the description in the Dtrs list of the active
% rule.
test_active_rule_and_recent_daughter(EdgeId) :-
        call_residue(active_edge(Start,End,Mother,RuleDtrs),AEdgeResidue),!,
        call_residue(edge(EdgeId,EdgeStart,NewEnd,Tag,SVs,_Dtrs,_RuleName),PEdgeResidue),
        ( (End =\= EdgeStart, discontinuous_edges(off)) ->
            sformat(EString,"The next daughter should start at position ~w not ~w!",[End,EdgeStart]),
            sformat(GString,"Die nächste Tochter sollte bei ~w beginnen, nicht bei ~w!",[End,EdgeStart]),
            write_warning(EString,GString)
        ; otherwise ->
            get_next_dtr_and_rest(RuleDtrs,Dtr,RestDtrs),

            chart_debug, % sets a flag that can be used by the user in the constraints
            ( ud(Dtr,Tag,SVs) ->
                ( PEdgeResidue \== [] ->
                    ( edge(EdgeId,EdgeStart,NewEnd,Tag,SVs,_Dtrs,_RuleName) ->
%                        call_or_freeze_again(PEdgeResidue) ->
                        write(user_error,'Unblocked goals of passive edge suceeded.'),nl(user_error),
                        PEdgeOkay = true
                    ; write(user_error,'Unblocked goals of the passive edge failed!'),nl(user_error),
                        %print_unblocked_goals(PEdgeResidue),
                        PEdgeOkay = false
                    )
                ; PEdgeOkay = true
                ),
                (PEdgeOkay ->
                    ( AEdgeResidue \== [] ->
                        ( active_edge(Start,End,Mother,RuleDtrs) ->
                            write(user_error,'Unblocked goals of active edge suceeded.'),nl(user_error),
                            assert_active_edge(Start,NewEnd,Mother,RestDtrs),
                            mark_edge(EdgeId,green)
                        ; write(user_error,'Unblocked goals of the active edge failed!'),nl(user_error)
                        )
                    ; assert_active_edge(Start,NewEnd,Mother,RestDtrs),
                        mark_edge(EdgeId,green)
                    )
                ; true
                )
                    
            ;   fs2desc(Dtr,DtrDesc),
                fs2desc(Tag-SVs,EdgeDesc),
                debug_unify(EdgeDesc,DtrDesc),
%                write(user_error,'Unification of Feature Structure without blocked constraints failed.'),nl(user_error),
%                write(user_error,'Checking for looping constraints.'),nl(user_error),
                timeout_time_(TimeOutTime),
                edge(EdgeId,EdgeStart,NewEnd,FreshTag,FreshSVs,_Dtrs,_RuleName),
                active_edge(Start,End,_FreshMother,FreshRuleDtrs),
                get_next_dtr_and_rest(FreshRuleDtrs,FreshDtr,_FreshRestDtrs),
                ( time_out(on_exception(Exception,ud(FreshDtr,FreshTag,FreshSVs),true),TimeOutTime,_Result) -> true
                ; Exception = none
                ),
                ( Exception == time_out  ->
                    sformat(ETimeOutWarning,'The unblocked constraints of the passive edge seem to be looping. (Timeout ~w msec)',[TimeOutTime]),
                    sformat(GTimeOutWarning,'Die deblockierten Constraints scheinen nicht zu terminieren. (Timeout ~w msec)',[TimeOutTime]),
                    write_warning(ETimeOutWarning,GTimeOutWarning)

%                   unfortunately we cannot print the stuff, since the recursion loops again ...
%                    print_unblocked_goals(AEdgeResidue),
%                    print_unblocked_goals(PEdgeResidue)
                ; true
                  %otherwise ->
                  %  write(user_error,'No looping constraints.'),nl(user_error)
                )
            ),
            nochart_debug
        ).        




% there is no active edge yet, so we are dealing with the first daughter
test_active_rule_and_recent_daughter(EdgeId) :-
        selected_rule(RuleId),
        sm_rule(RuleId,RuleName,Reference),
        clause(alec_rule(RuleName,DtrsDesc,_,MotherDesc,_,_),true,Reference),
        satisfy_dtrs(DtrsDesc,DtrCats,[],RuleDtrs,gdone),
        add_to(MotherDesc,TagMoth,bot),

        CatsOut = [TagMoth-bot|DtrCats],
        extensionalise_list(CatsOut),

        call_goals(RuleDtrs,RestRuleDtrs),
        get_next_dtr_and_rest(RestRuleDtrs,Dtr,RestDtrs),

        edge(EdgeId,Start,End,Tag,SVs,_Dtrs,_RuleName),
        
        ( ud(Dtr,Tag,SVs) ->
            assert_active_edge(Start,End,TagMoth,RestDtrs),
            enable_show_active_edge,
            mark_edge(EdgeId,green)
        ; %write_warning('Unification failed'),
          % this warning blocks the chart display and depending on the window manager
          % the user does not see it since the windows from the diffs block the view
            fs2desc(Dtr,DtrDesc),
            fs2desc(Tag-SVs,EdgeDesc),
            debug_unify(EdgeDesc,DtrDesc)
        ).        


print_unblocked_goals([]).
print_unblocked_goals([Vars-Goal|Rest]) :-
      ( nonvars(Vars) ->
%          write(user_error,'Calling goal: '),
%          write(user_error,Goal),nl(user_error),
          empty_assoc(AssocIn),
          residue_args([Vars-Goal],ResArgs,_ArgsOut),
          duplicates_list(ResArgs,AssocIn,DupsIn,AssocIn,VisIn,0,_NumMid),
          pp_resgoal(Goal,DupsIn,_,VisIn,_,_HDIn,_)
      ; true
      ),
      print_unblocked_goals(Rest).
        


%This does not work since the variables seems to be disociated from the ones
%we used in the unification.

call_or_freeze_again([]).
call_or_freeze_again([Vars-Goal|Rest]) :-
        ( nonvars(Vars) ->
            write(user_error,'Calling goal: '),
            write(user_error,Goal),nl(user_error),
%            empty_assoc(AssocIn),
%            residue_args([Vars-Goal],ResArgs,_ArgsOut),
%            duplicates_list(ResArgs,AssocIn,DupsIn,AssocIn,VisIn,0,_NumMid),
%            pp_resgoal(Goal,DupsIn,_,VisIn,_,_HDIn,_),
            !,
            call(Goal),
            write(user_error,' Okay.'), nl(user_error)
        ; freeze_vars(Vars,Goal)
        ),
        call_or_freeze_again(Rest).

nonvars([]).
nonvars([Var|Rest]) :-
        nonvar(Var),
        nonvars(Rest).

freeze_vars([],_).
freeze_vars([Var|Rest],Goal) :-
        freeze(Var,Goal),
        freeze_vars(Rest,Goal).   


get_next_dtr_and_rest([Dtr1|Dtrs],Dtr,DtrsRest) :-
        ( Dtr1 = (cat>Dtr),
            DtrsRest = Dtrs
        ; Dtr1 = (sem_head>Dtr),
            DtrsRest = Dtrs
        ; Dtr1 = (cats>[Dtr|MoreCats]),    % not properly tested
            ( MoreCats = [] ->
                DtrsRest = Dtrs
            ; otherwise ->
                DtrsRest = [cats>MoreCats|Dtrs]
            )
        ).

call_goals([cat>X|Rest], [cat>X|Rest]) :- !.
call_goals([cats>X|Rest],[cats>X|Rest]) :- !.
call_goals([sem_head>X|Rest],[sem_head>X|Rest]) :- !.

call_goals([],[]).
call_goals([goal>Goal|R1],R2) :-
        ( query_goal(Goal) -> true
        ; write(user_error,'Constraint Application failed:'), write(user_error,Goal), nl(user_error)
        ),
        call_goals(R1,R2).



% if all daughters were found print the structure
% and do not assert anything.
assert_active_edge(Start,End,MTag-MSVs,Dtrs) :-
        call_goals(Dtrs,DtrsRest),
        ( DtrsRest = [] ->

            ((current_predicate(portray_cat,portray_cat(_,_,_,_,_)), % see also gen/1 - portray_cat/5
              portray_cat('Passive Edge',bot,MTag,MSVs,_Residue)) -> true %  can be called with var 1st arg.
            ; nl, write('Passive Edge: '),nl, ttyflush,
                pp_fs_res(MTag-MSVs,_SVs,_Residue), nl
            )
        ; assert(active_edge(Start,End,MTag-MSVs,DtrsRest))
        ).


%-----------------

%:- dynamic rule_counter/1.   % in start_chart definiert

% ale uses rule/2. so we have to use something differnt

:- dynamic sm_rule/3.


register_rules :-
  retractall(sm_rule(_,_,_)),
  retractall(rule_counter(_)),
  assert(rule_counter(-1)),
  foreach(get_rule_name(RuleName,Reference),
          register_rule(RuleName,Reference)
         ).

% taken from rule/1 in ale.pl
% this is not perfect since it normalizes disjunctions in macro
% specifications and therefore produces two rule names for a rule
% that is stored as one rule in alec_rule. We have to keep the
% reference around and only register the rule if the reference
% differs.
get_rule_name(RuleName,Reference) :-
        clause(alec_rule(RuleName,DtrsDesc,_,Moth,_,_),true,Reference),
        call_residue((satisfy_dtrs(DtrsDesc,DtrCats,[],_Dtrs,gdone),
                      (secret_noadderrs
                      ; secret_adderrs,
                          fail),
                      add_to(Moth,TagMoth,bot),
                      CatsOut = [TagMoth-bot|DtrCats],
                      extensionalise_list(CatsOut)),_Residue).

%  write(Reference),nl,
  
%  rule_counter(RC),
%  RuleNumber is RC + 2,
 
%  getUniqueName(RuleName,RuleNumber, UniqueRuleName).

register_rule(_,Reference) :-
        sm_rule(_,_,Reference),!.   % registered allready

register_rule(RuleName,Reference) :-
  retract(rule_counter(C)),
  EdgeId is C + 1,
  assert(rule_counter(EdgeId)),
  assert(sm_rule(EdgeId,RuleName,Reference)).

% Zeichne alle Regeln, die mit register_rule registriert wurden
%

draw_rules :-
  to_wish_stream(TO_WISH_S),
  foreach(sm_rule(EdgeId,RuleName,_),
          %format('draw_edge 0 ~w -1 -1 ~w 0 0 {}~',[EdgeId,RuleName])
          format_wish(TO_WISH_S, 'draw_edge 0 ~w -1 -1 "~w" 0 0 {}~n',[EdgeId,RuleName])
         ).


%--------------------------------------------------------------

print_active_edge :-
  call_residue(active_edge(_Start,_End,MTag-MSVs,_DtrsRest),Residue),
  ((current_predicate(portray_cat,portray_cat(_,_,_,_,_)), % see also gen/1 - portray_cat/5
              portray_cat('Active Edge',bot,MTag,MSVs,Residue)) -> true %  can be called with var 1st arg.
            ; nl, write('Active Edge: '),nl, ttyflush,
                pp_fs_res(MTag-MSVs,_SVs,Residue), nl
            ).

print_active_edge :-
  write_warning('There is no active edge!~n','Es gibt keine aktive Kante!~n').



getUniqueName(RuleName,RuleNumber, UniqueRuleName) :-
  name(RuleName,RNS),
  name(RuleNumber,RNoS),

  xappend(RNS,[32,47,32|RNoS],URNS),

  name(UniqueRuleName,URNS).


%----------------------------------------------

view_compact :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'view_compact 0~n').

view_timebased :-
  to_wish_stream(TO_WISH_S),
  format_wish(TO_WISH_S, 'view_timebased 0~n').

%-----------------------------------------------

% Wenn ein Nutzer das Chart-Display zu macht, wird auch
% der WISH-Prozess beendet.

quit_parser(_ChartId) :-
        shutdown_wish.

% beim zweiten Display scheint dann kein quit Signal mehr zu kommen


print_fs_id(EdgeId) :-
        ( call_residue(clause(edge(EdgeId,_Start,_End,Tag,SVs,_Dtrs,_RuleName),true),Residue)
        ; call_residue(empty_cat(EdgeId,_N,Tag,SVs,_Dtrs,_RuleName),Residue)
        ),
        ((current_predicate(portray_cat,portray_cat(_,_,_,_,_)),  % see also gen/1 - portray_cat/5
           get_phon(Tag-SVs,Phon),
           portray_cat(Phon,bot,Tag,SVs,Residue)) -> true         %  can be called with var 1st arg.
         ; nl, write('CATEGORY: '),nl, ttyflush,
           pp_fs_res(Tag,SVs,Residue), nl
         ).


/*
de :- draw_edges_after_parsing.

draw_edges_after_parsing :-
   init_chart_display_if_on,
   register_edges,
   draw_edges,
   finish_chart_display_if_on.


draw_edges :-
   edge(EdgeId,Left,Right,_TagOut,_SVsOut,Dtrs,RuleName),

   draw_item(EdgeId,Left,Right,RuleName,Dtrs),fail.

draw_edges.
*/

map_dtrs([],[]).
map_dtrs([AleId|AleR],[ChartId|ChartR]) :-
        chart_ale_id(ChartId,AleId),!,
%        sformat(String,"Mapping ~w to ~w.",[AleId,ChartId]),
%        write(String),nl,
        map_dtrs(AleR,ChartR).

map_dtrs([AleId|AleR],[AleId|ChartR]) :-
        map_dtrs(AleR,ChartR).


draw_empty_cats(_) :-
        chart_display(off),!.

draw_empty_cats(L) :-
        register_empty_cats(L),
        draw_registered_empty_cats(L).

register_empty_cats(-1) :- !.
register_empty_cats(L) :-
  Ln is L - 1,
  foreach(call_residue(empty_cat(AleId,L,_Tag,_SVs,_Dtrs,_RuleName),_Residue),
          register_empty(empty(AleId,L))),  
  register_empty_cats(Ln).


register_empty(AleId) :-
   gennum(Id),
   assert(chart_ale_id(Id,AleId)).

draw_registered_empty_cats(-1) :- !.
draw_registered_empty_cats(L) :-
          Ln is L - 1,
  foreach(call_residue(empty_cat(EdgeId,L,_Tag,_SVs,Dtrs,RuleName),_Residue),
          (chart_ale_id(DisplayId,empty(EdgeId,L)),
           draw_item(DisplayId,L,L,RuleName,Dtrs))),
  draw_registered_empty_cats(Ln).



% also covers multiword lexemes
% not used yet
get_words_from_parse_string(Start,End,Words) :-
  clause(parsing(String), true),
  get_words_from_to(Start,End,String,0,Words).

get_words_from_to(Start,End,[_|T],Pos,Words) :-
        Pos < Start,
        PosN is Pos + 1,
        get_words_from_to(Start,End,T,PosN,Words).

get_words_from_to(Start,End,[H|_],Pos,[H]) :-
        Pos = Start,
        PosN is Pos + 1,
        PosN = End.

get_words_from_to(Start,End,[H|T],Pos,[H|Words]) :-
        Pos = Start,
        PosN is Pos + 1,
        PosN < End,
        get_words_from_to(Start,End,T,Pos,Words).


% this records whether we are in the Prolog mode or in the interactive trale mode.
% it is needed to be able to leave the trale mode on request from the EMACS menu
% and to some Prolog actions and return to the former mode.

:- dynamic trale_mode/1.

trale_mode(prolog).

prompt :-
        write('>>>   '), flush_output.

go :- retractall(trale_mode(_)),
        assert(trale_mode(trale)),
        go_.
go. % see end_i for setting the trale_mode/1 flag.

go_ :- prompt,
       check_chart_display,
       read_line_of_words(S,Desc),
       !, end_i(S),               % fails if S = ende
       ( interprete(S) -> true
       ; parse_print_all(S,Desc)
       ),
       write(' '),nl, % you will not believe this:
                      % writing out this space makes the prompt functionality in the sicstus emacs interface for '>>>'
                      % work. Otherwise it jumps one line too far.
       !,go_.

check_chart_display :-
  chart_display(off),!.


check_chart_display :-
  socket_select([],_,0:1,[user_input],Streams),
  Streams = [_].


check_chart_display :-
  from_wish_stream(FROM_WISH),
  socket_select([],_,0:1,[FROM_WISH],Streams),
  ( Streams = []                                    % no input continue looping
  ; %write(Term), flush, 
      catch(read(FROM_WISH,Term),
            _,
            reset_wish),
%      write(Term), flush_output,
      ( Term = end_of_file -> reset_wish   % display was closed with kill
      ; otherwise -> call(Term)
      )
  ; true      % falls Term fehlschlaegt
  ),!,
  check_chart_display.

% Wish ist noch nicht gestartet
check_chart_display :-
  check_chart_display.


% some commands the user may use to go back to the Prolog mode
end_i([Word]) :- 
  ( Word = ende
    ;
        Word = end
    ;
        Word = stop
    ),
        inform_user_about_go,
  retractall(trale_mode(_)),assert(trale_mode(prolog)),
        !,fail.

% the command that is used by the EMACS interface, It tells us to switch
% to prolog.
end_i([prolog_mode]) :- 
        !,fail.

end_i(_).

goto_old_mode :-
  ( trale_mode(trale) -> go
  ; true).

% if we are in Prolog mode already the following command is executed directly.
prolog_mode.

interprete(S) :-
        ( S = [C,'.']
        ; S = [C]
        ),
        \+ (current_predicate(lex/2),  % maybe lexicon compilation failed
               lex(C,_)),
        functor(C,Pred,Arity),
        current_predicate(Pred/Arity),
        (catch(call(C),
               _Catcher,
               ( language(german) -> format(user_error,'~n*Exception: Kommando fehlgeschlagen.~n~n',[])
               ; format(user_error,'~n* Exception: Command did not succedd.~n~n',[])
               ))
        ;true).                 % if you tried to call it do not fail.

list2list_desc(S,List,Desc) :-
        del_last(S,El,List),
        (El = '.',!,
            (current_predicate(decl_symbol,decl_symbol(_)) ->
                decl_symbol(Desc)
            ; Desc = bot
            )
        ; El = '!',!,
            (current_predicate(imp_symbol,imp_symbol(_)) ->
                imp_symbol(Desc)
            ; Desc = bot
            )
        ; El = '?',!,
            (current_predicate(interrog_symbol,interrog_symbol(_)) ->
                interrog_symbol(Desc)
            ; Desc = bot
            )
        ; (current_predicate(root_symbol,root_symbol(_)) ->
              root_symbol(Desc)
          ; Desc = bot
          )
        ).
        
:- dynamic found_s/0.

parse_print_all(S,Desc) :-
        ( S = []   % no input
        ; write('Parsing ...'),nl,
            reset_timer,
            rec_without_q(S,Desc),
            timer(T), write_result(T,_ExpSol),nl
        ).
%        draw_edges_after_parsing.


% this is like rec, but does not ask questions before backtracking
%
% also took stuff from test_suite_handling for counting the residue

rec_without_q(Words,Desc) :-
%      init_chart_display_if_on,  
  retractall(num_res(_,_)),
  retractall(num_scopings(_,_)),
  retractall(num_sol(_)),
  asserta(num_sol(0)),
%       retractall(id(_)),
       retractall(chart_ale_id(_,_)),
%       retractall(sm_edge(_EdgeId,_AleId,_Start,_End,_Dtrs,_RuleName)),
%       rule_counter(RC), FirstId is RC + 1, assert(id(FirstId)),

   \+ \+ (catch(rec(Words,Tag,SVs,Desc,Residue),
                Exception,
                (( Exception = ale(Ex) -> soft_alex(Ex)
                 ; otherwise -> nl(user_error),write(user_error,Exception),nl(user_error),nl(user_error)
                 ),
                    fail)),
             ( fs(on) -> 
                 ((current_predicate(portray_cat,portray_cat(_,_,_,_,_)), % see also gen/1 - portray_cat/5
                   portray_cat(Words,bot,Tag,SVs,Residue)) -> true %  can be called with var 1st arg.
                 ; nl, write('CATEGORY: '),nl, ttyflush,
                     pp_fs_res(Tag,SVs,Residue), nl
                 )
             ; true),
             ( mrs(on) ->
                 print_mrs(Tag-SVs)
             ; true),
             ( (display_mrs(on), scopable_description_(Scopable),member(Desc,Scopable)) ->
                 mrs_utool(display,Tag-SVs,ErrorMessage),
                 ( (debug_mrs(on),ErrorMessage \== "") ->
                     format(user_error,"~N* ERROR: ~s~n~n",[ErrorMessage])
                 ; true
                 )
             ; true),
             
             retract(num_sol(Num)),
 NewNum is Num + 1,
 length(Residue,ResNum),
 asserta(num_res(NewNum,ResNum)),
 ( (scope_mrs(on), scopable_description_(Scopable),member(Desc,Scopable)) ->
     mrs_utool(solvable,Tag-SVs,Scopings,ErrorMessage),
     asserta(num_scopings(NewNum,Scopings))
 ; true),
 asserta(num_sol(NewNum)),   
         fail).

rec_without_q(_,_) :-
    finish_chart_display_if_on.

% the following is alex from ale.pl without an abort at the end:
soft_alex(Exception) :-
  format(user_error,'{ALE: ERROR: ',[]),
  ale_exception(Exception),
  format(user_error,'}~n~n',[]),
  flush_output(user_error).

del_last([El],El,[]) :- !.
del_last([H|T],El,[H|Rest]) :-
   del_last(T,El,Rest).

print_fs_overview(SVs) :-
        SVs =.. [Functor|Args],
        write(Functor),write(' '),
        print_args_overview(Args),
        nl.

print_args_overview([]).
print_args_overview([_-H|T]) :-
        H =.. [Functor|Args],
        write(Functor),write('('),print_args_overview2(Args),write(')'),nl,
        print_args_overview(T).

print_args_overview2([]).
print_args_overview2([_-H|T]) :-
        H =.. [Functor|_Args],
        write(Functor),write(' '),
        print_args_overview2(T).


:- dynamic counter/1.
 
counter(0).

count(Clause/Arity,C) :-
  functor(Call,Clause,Arity), 
  count_(Call,C),!.

count_(Clause,_) :- 
  Clause,      
  retract(counter(C)),
  C1 is C + 1,
  assert(counter(C1)),
  fail.
 
count_(_,C)  :- 
  retract(counter(C)),
  assert(counter(0)).



foreach(X,Y) :-
   X, once(Y), fail.
foreach(_,_). 


:- dynamic mynum/1.

mygennum(Num) :-
        retract(mynum(X)),
        Num is X + 1,
        assert(mynum(Num)).

print_liszt(FS) :-
        deref(FS,Ref,SVs),
        featval(hd,SVs,Ref,FirstFS),
        pp_fs(FirstFS),
        featval(tl,SVs,Ref,RestFS),
        print_liszt(RestFS).

print_liszt(_).

path2list(X:Y,[X|L]) :-
        path2list(Y,L).
path2list(X,[X]).

% remove leftover stuff
:- shutdown_wish.



inform_user_about_top_level :-
        (language(german) -> format(user_error,"~nSie können das Lexikon und die Grammatik verändern.~n",[]),
            format(user_error,"Wenn Sie `c.' eingeben, werden die Grammatik und das Lexikon neu geladen.~n",[]),
            format(user_error,"Im Menue `Trale' können Sie Hierarchien und Signaturen ausgeben (z.B. vom Typ `bot').~n",[]),
%            format(user_error,"Mit `ende.' können Sie den interaktiven Modus verlassen.~n",[]),
%            format(user_error,"Sie können sich dann z. B. mit `draw_hierarchy(bot).' alle Untertypen von `bot' ansehen.~n",[]),
            format(user_error,"~nBitte geben Sie einen Satz ein.~n~n",[])
        ; format(user_error,"~nYou may change the lexicon and the grammar.~n",[]),
            format(user_error,"Type `c.' to reload grammar and lexicon.~n",[]),
%            format(user_error,"Type `end.' to leave the interactive mode.~n",[]),
            format(user_error,"Use the `Trale' menu to draw hierarchies or signatures (for instance for the type `bot').~n",[]),
            format(user_error," In the non-interactive mode, you may type `draw_hierarchy(bot).' to display the type hierarchy below the type `bot'.~n",[]),
            format(user_error,"~nPlease enter a sentence.~n~n",[])
        ).

inform_user_about_go :-
        (language(german) -> format(user_error,"~nGeben Sie `go.' ein, um in den interaktiven Modus zurückzukehren.~n",[])
        ; format(user_error,"~nType `go.' to return to the interactive mode.~n",[])
        ).




wrapped_gen(Desc) :-
        ( no_questions,
            reset_timer,
            gen(Desc)
        ;   timer(T),
            msec2time(T,Time),
            format(user_error,'~n~n ==> ~w CPU time.~n', [Time]),
            write(' '),nl,prompt, % see go/0.
            ask_questions
        ).


% the same as above, but uses g/1 instead of gen/1.
wrapped_g(Desc) :-
        ( no_questions,
            reset_timer,
            g(Desc),
            timer(T),
            msec2time(T,Time),
            format(user_error,'~n~n ==> ~w CPU time.~n', [Time]),
            write(' '),nl,prompt, % see go/0.
            ask_questions
        ).


% takes a list of pathes and provides a description
% that contains all the information from the values of the pathes.
make_path_desc(PathL,Ref,SVs,FullDesc) :-
        pathval(PathL,Ref,SVs,SRef,SSVs) ->
        fs2desc(SRef-SSVs,Desc),
        get_full_desc(PathL,Desc,FullDesc).


make_generation_desc([Path],Ref,SVs,Desc) :-
        make_path_desc(Path,Ref,SVs,Desc).

make_generation_desc([Path|Pathes],Ref,SVs,(Desc,Descs)) :-
  make_path_desc(Path,Ref,SVs,Desc),
  make_generation_desc(Pathes,Ref,SVs,Descs).

get_full_desc([],Desc,Desc).
get_full_desc([First|Rest],Desc,First:FullDesc) :-
        get_full_desc(Rest,Desc,FullDesc).

/*
dtr_to_desc([cat> Desc],     [Desc]) :- !.
dtr_to_desc([sem_head> Desc],[Desc]) :- !.
dtr_to_desc([cats> Descs],   Descs)  :- !.

dtr_to_desc(X,X) :- write(X).


dtrs_to_desc_list([],[]).
dtrs_to_desc_list([In|InRest],[Out|OutRest]) :- 
   dtr_to_desc(In,Out),
   dtrs_to_desc_list(InRest,OutRest).
*/

% not implemented yet.
get_phon(FS,Phon) :- 
  phon_feat_(PhonFeat),
  feat_fs2featval(PhonFeat,FS,PhonFS),
  get_phon_atom_list(PhonFS,PhonList),
  wordlist2string(PhonList,String),
  name(Phon,String).

get_phon_atom_list(_-e_list,[]).

get_phon_atom_list(PhonFS,[First|PhonT]) :-
  feat_fs2featval(hd,PhonFS,_-(a_ First)),
  feat_fs2featval(tl,PhonFS,PhonFST),
  get_phon_atom_list(PhonFST,PhonT).