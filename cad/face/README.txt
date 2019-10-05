Steps to create the face model.

1) Starting with the Poppy skeleton/head/head_face.stl
2) Import 1) into Blender. Scale to mm. Set origin, rotation. Remove all non-manifold
   vertices. Save as face_raw.blend. Export as face_raw.stl.
3) Run OpenScad, blocked_face.scad. This reads head_face.stl and blocks off portions of
   the face to be saved. Export the block shapes only as blocks.stl.
4) Import 2) and 3) into Blender. Use boolean operators to save the intersection of 
   the blocks and the raw face. Clean up any block remnants. Save as collar.blend 
   and export as collar.stl.
5) Fill the top open space by selecting points around the perimeter and "new face 
   from vertices". For each of the three faces, loop-cut and separate. Save as 
   collar-face.blend.
6) From 5) Delete the collar (We will merge at the end). Merge the 3 faces into
   a single object. Lock down perimeter. Save as face0.blend. 
