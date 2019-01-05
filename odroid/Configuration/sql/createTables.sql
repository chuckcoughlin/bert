-- Copyright 2019. Charles Coughlin. All rights reserved.
-- These tables are used by the robot to access poses, state
-- arrays and action lists.
--
-- The "Motors" table contains a list of motor names and ids.
CREATE	TABLE Motor (
  id	int	PRIMARY	KEY,
  name	text	NOT NULL
);
