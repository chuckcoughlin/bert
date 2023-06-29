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
   a single object. Lock down perimeter. Save as face-only.blend. 
7) Select the outer edge of the three planes. From the "Mesh" menu add a convex hull. 
   Delete the part of the hull that is on the backside. From the "vertex" menu
   select "bevel", in turn, for each of the two nodes that radiate a large number
   of edges. Save as "face-mesh'.

Alternate
Files named cartoon_head have been purchased from TurboSquid under their 3D Model License (https://blog.turbosquid.com/turbosquid-3d-model-license). Product ID: 1912829. "You can use the model for both non-commercial and commercial purposes. You can use it for multiple projects, forever(more informations under license)."
