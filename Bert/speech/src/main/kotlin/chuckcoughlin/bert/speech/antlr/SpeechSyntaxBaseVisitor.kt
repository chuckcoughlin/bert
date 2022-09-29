// Generated from SpeechSyntax.g4 by ANTLR 4.7.2
package chuckcoughlin.bert.speech.antlr

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor

/**
 * This class provides an empty implementation of [SpeechSyntaxVisitor],
 * which can be extended to create a visitor which only needs to handle a subset
 * of the available methods.
 *
 * @param <T> The return type of the visit operation. Use [Void] for
 * operations with no return type.
</T> */
open class SpeechSyntaxBaseVisitor<T> : AbstractParseTreeVisitor<T>(), SpeechSyntaxVisitor<T?> {
    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitLine(ctx: LineContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitStatement(ctx: SpeechSyntaxParser.StatementContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleGreeting(ctx: HandleGreetingContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitInitializeJoints(ctx: InitializeJointsContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitConfigurationRequest(ctx: ConfigurationRequestContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleBulkPropertyRequest(ctx: HandleBulkPropertyRequestContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleListCommand1(ctx: HandleListCommand1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleListCommand2(ctx: HandleListCommand2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMoveMotor(ctx: MoveMotorContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMoveSpeed(ctx: MoveSpeedContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitEnableTorque(ctx: EnableTorqueContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitSetMotorPosition(ctx: SetMotorPositionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitSetMotorProperty(ctx: SetMotorPropertyContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitStraightenJoint(ctx: StraightenJointContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleArbitraryCommand(ctx: HandleArbitraryCommandContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitAttributeQuestion(ctx: AttributeQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitConfigurationQuestion(ctx: ConfigurationQuestionContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleBulkPropertyQuestion(ctx: HandleBulkPropertyQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitJointPropertyQuestion(ctx: JointPropertyQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMotorPropertyQuestion1(ctx: MotorPropertyQuestion1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMotorPropertyQuestion2(ctx: MotorPropertyQuestion2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMetricsQuestion(ctx: MetricsQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitPoseQuestion(ctx: PoseQuestionContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitLimbLocationQuestion(ctx: LimbLocationQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitWhyMittens(ctx: WhyMittensContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitDeclarePose1(ctx: DeclarePose1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitDeclarePose2(ctx: DeclarePose2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitDeclareNoNamePose(ctx: DeclareNoNamePoseContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand1(ctx: MapPoseToCommand1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand2(ctx: MapPoseToCommand2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand3(ctx: MapPoseToCommand3Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand4(ctx: MapPoseToCommand4Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand5(ctx: MapPoseToCommand5Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitWordList(ctx: WordListContext): T? {
        return visitChildren(ctx)
    }
}