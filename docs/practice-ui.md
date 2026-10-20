# Practice workspace

The page uses a responsive workspace layout with practice and the concept map side
by side on wide screens, stacking below 1050px. Progress details span the full
width. Below 640px, summary metrics become compact rows and the navigation wraps.
The table and graph have local horizontal scrolling rather than widening the page.

Summary metrics use the existing concept-statistics response: attempts summed
across mapped concepts, accuracy calculated from summed correct/total attempts,
and mastered concepts out of the total. Unassigned attempts are excluded. Values
refresh after saved answers and show an unavailable state on request failure.

Navigation links target practice, the concept map, and progress. Answer options
retain native radio controls and a fieldset legend. The layout includes visible
keyboard focus, reduced-motion support, named scroll regions, and text labels in
addition to colors. Mastery rules are available in a native disclosure below the
table. No external fonts, icon libraries, or new API requests are required.

Verification: 13 frontend checks pass, including updated summary values and
existing answer, retry, statistics, prerequisite, and graph behavior. HTML IDs,
navigation targets, and CSS structure were checked; the running app serves the
page and stylesheet. Visual browser review remains outstanding because no browser
connection is available.
