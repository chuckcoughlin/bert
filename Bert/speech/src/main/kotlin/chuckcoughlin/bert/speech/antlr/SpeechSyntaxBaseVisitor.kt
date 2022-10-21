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
    override fun visitLine(ctx: SpeechSyntaxParser.LineContext?): T? {
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
    override fun visitHandleGreeting(ctx: SpeechSyntaxParser.HandleGreetingContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitInitializeJoints(ctx: SpeechSyntaxParser.InitializeJointsContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitConfigurationRequest(ctx: SpeechSyntaxParser.ConfigurationRequestContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleBulkPropertyRequest(ctx: SpeechSyntaxParser.HandleBulkPropertyRequestContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleListCommand1(ctx: SpeechSyntaxParser.HandleListCommand1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleListCommand2(ctx: SpeechSyntaxParser.HandleListCommand2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMoveMotor(ctx: SpeechSyntaxParser.MoveMotorContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMoveSpeed(ctx: SpeechSyntaxParser.MoveSpeedContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitEnableTorque(ctx: SpeechSyntaxParser.EnableTorqueContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitSetMotorPosition(ctx: SpeechSyntaxParser.SetMotorPositionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitSetMotorProperty(ctx: SpeechSyntaxParser.SetMotorPropertyContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitStraightenJoint(ctx: SpeechSyntaxParser.StraightenJointContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleArbitraryCommand(ctx: SpeechSyntaxParser.HandleArbitraryCommandContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitAttributeQuestion(ctx: SpeechSyntaxParser.AttributeQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitConfigurationQuestion(ctx: SpeechSyntaxParser.ConfigurationQuestionContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitHandleBulkPropertyQuestion(ctx: SpeechSyntaxParser.HandleBulkPropertyQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitJointPropertyQuestion(ctx: SpeechSyntaxParser.JointPropertyQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMotorPropertyQuestion1(ctx: SpeechSyntaxParser.MotorPropertyQuestion1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMotorPropertyQuestion2(ctx: SpeechSyntaxParser.MotorPropertyQuestion2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMetricsQuestion(ctx: SpeechSyntaxParser.MetricsQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitPoseQuestion(ctx: SpeechSyntaxParser.PoseQuestionContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitLimbLocationQuestion(ctx: SpeechSyntaxParser.LimbLocationQuestionContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitWhyMittens(ctx: SpeechSyntaxParser.WhyMittensContext?): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitDeclarePose1(ctx: SpeechSyntaxParser.DeclarePose1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitDeclarePose2(ctx: SpeechSyntaxParser.DeclarePose2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitDeclareNoNamePose(ctx: SpeechSyntaxParser.DeclareNoNamePoseContext): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand1(ctx: SpeechSyntaxParser.MapPoseToCommand1Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand2(ctx: SpeechSyntaxParser.MapPoseToCommand2Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand3(ctx: SpeechSyntaxParser.MapPoseToCommand3Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand4(ctx: SpeechSyntaxParser.MapPoseToCommand4Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitMapPoseToCommand5(ctx: SpeechSyntaxParser.MapPoseToCommand5Context): T? {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     *
     * The default implementation returns the result of calling
     * [.visitChildren] on `ctx`.
     */
    override fun visitWordList(ctx: SpeechSyntaxParser.WordListContext): T? {
        return visitChildren(ctx)
    }
}