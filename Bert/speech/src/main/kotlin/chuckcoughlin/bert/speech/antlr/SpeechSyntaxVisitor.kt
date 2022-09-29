// Generated from SpeechSyntax.g4 by ANTLR 4.7.2
package chuckcoughlin.bert.speech.antlr

import org.antlr.v4.runtime.tree.ParseTreeVisitor

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by [SpeechSyntaxParser].
 *
 * @param <T> The return type of the visit operation. Use [Void] for
 * operations with no return type.
</T> */
interface SpeechSyntaxVisitor<T> : ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by [SpeechSyntaxParser.line].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitLine(ctx: LineContext?): T

    /**
     * Visit a parse tree produced by [SpeechSyntaxParser.statement].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitStatement(ctx: SpeechSyntaxParser.StatementContext?): T

    /**
     * Visit a parse tree produced by the `handleGreeting`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitHandleGreeting(ctx: HandleGreetingContext?): T

    /**
     * Visit a parse tree produced by the `initializeJoints`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitInitializeJoints(ctx: InitializeJointsContext?): T

    /**
     * Visit a parse tree produced by the `configurationRequest`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitConfigurationRequest(ctx: ConfigurationRequestContext?): T

    /**
     * Visit a parse tree produced by the `handleBulkPropertyRequest`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitHandleBulkPropertyRequest(ctx: HandleBulkPropertyRequestContext): T

    /**
     * Visit a parse tree produced by the `handleListCommand1`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitHandleListCommand1(ctx: HandleListCommand1Context): T

    /**
     * Visit a parse tree produced by the `handleListCommand2`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitHandleListCommand2(ctx: HandleListCommand2Context): T

    /**
     * Visit a parse tree produced by the `moveMotor`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMoveMotor(ctx: MoveMotorContext): T

    /**
     * Visit a parse tree produced by the `moveSpeed`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMoveSpeed(ctx: MoveSpeedContext): T

    /**
     * Visit a parse tree produced by the `enableTorque`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitEnableTorque(ctx: EnableTorqueContext): T

    /**
     * Visit a parse tree produced by the `setMotorPosition`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitSetMotorPosition(ctx: SetMotorPositionContext): T

    /**
     * Visit a parse tree produced by the `setMotorProperty`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitSetMotorProperty(ctx: SetMotorPropertyContext): T

    /**
     * Visit a parse tree produced by the `straightenJoint`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitStraightenJoint(ctx: StraightenJointContext): T

    /**
     * Visit a parse tree produced by the `handleArbitraryCommand`
     * labeled alternative in [SpeechSyntaxParser.command].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitHandleArbitraryCommand(ctx: HandleArbitraryCommandContext): T

    /**
     * Visit a parse tree produced by the `attributeQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitAttributeQuestion(ctx: AttributeQuestionContext): T

    /**
     * Visit a parse tree produced by the `configurationQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitConfigurationQuestion(ctx: ConfigurationQuestionContext?): T

    /**
     * Visit a parse tree produced by the `handleBulkPropertyQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitHandleBulkPropertyQuestion(ctx: HandleBulkPropertyQuestionContext): T

    /**
     * Visit a parse tree produced by the `jointPropertyQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitJointPropertyQuestion(ctx: JointPropertyQuestionContext): T

    /**
     * Visit a parse tree produced by the `motorPropertyQuestion1`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMotorPropertyQuestion1(ctx: MotorPropertyQuestion1Context): T

    /**
     * Visit a parse tree produced by the `motorPropertyQuestion2`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMotorPropertyQuestion2(ctx: MotorPropertyQuestion2Context): T

    /**
     * Visit a parse tree produced by the `metricsQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMetricsQuestion(ctx: MetricsQuestionContext): T

    /**
     * Visit a parse tree produced by the `poseQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitPoseQuestion(ctx: PoseQuestionContext?): T

    /**
     * Visit a parse tree produced by the `limbLocationQuestion`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitLimbLocationQuestion(ctx: LimbLocationQuestionContext): T

    /**
     * Visit a parse tree produced by the `whyMittens`
     * labeled alternative in [SpeechSyntaxParser.question].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitWhyMittens(ctx: WhyMittensContext?): T

    /**
     * Visit a parse tree produced by the `declarePose1`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitDeclarePose1(ctx: DeclarePose1Context): T

    /**
     * Visit a parse tree produced by the `declarePose2`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitDeclarePose2(ctx: DeclarePose2Context): T

    /**
     * Visit a parse tree produced by the `declareNoNamePose`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitDeclareNoNamePose(ctx: DeclareNoNamePoseContext): T

    /**
     * Visit a parse tree produced by the `mapPoseToCommand1`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMapPoseToCommand1(ctx: MapPoseToCommand1Context): T

    /**
     * Visit a parse tree produced by the `mapPoseToCommand2`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMapPoseToCommand2(ctx: MapPoseToCommand2Context): T

    /**
     * Visit a parse tree produced by the `mapPoseToCommand3`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMapPoseToCommand3(ctx: MapPoseToCommand3Context): T

    /**
     * Visit a parse tree produced by the `mapPoseToCommand4`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMapPoseToCommand4(ctx: MapPoseToCommand4Context): T

    /**
     * Visit a parse tree produced by the `mapPoseToCommand5`
     * labeled alternative in [SpeechSyntaxParser.declaration].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitMapPoseToCommand5(ctx: MapPoseToCommand5Context): T

    /**
     * Visit a parse tree produced by the `wordList`
     * labeled alternative in [SpeechSyntaxParser.phrase].
     * @param ctx the parse tree
     * @return the visitor result
     */
    fun visitWordList(ctx: WordListContext): T
}