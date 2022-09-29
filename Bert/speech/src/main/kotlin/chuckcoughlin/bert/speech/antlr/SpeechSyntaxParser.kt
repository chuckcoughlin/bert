// Generated from SpeechSyntax.g4 by ANTLR 4.7.2
package chuckcoughlin.bert.speech.antlr

import org.antlr.v4.runtime.atn.*

class SpeechSyntaxParser(input: TokenStream?) : Parser(input) {
    @get:Deprecated("")
    val tokenNames: Array<String?>
        get() = Companion.tokenNames
    val vocabulary: Vocabulary
        get() = VOCABULARY
    val grammarFileName: String
        get() = "SpeechSyntax.g4"
    val ruleNames: Array<String>
        get() = Companion.ruleNames
    val aTN: ATN
        get() = _ATN

    class LineContext(parent: ParserRuleContext?, invokingState: Int) : ParserRuleContext(parent, invokingState) {
        fun statement(): StatementContext {
            return getRuleContext(StatementContext::class.java, 0)
        }

        fun EOF(): TerminalNode {
            return getToken(EOF, 0)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitLine(this) else visitor.visitChildren(
                this
            )
        }
    }

    @Throws(RecognitionException::class)
    fun line(): LineContext {
        val _localctx = LineContext(_ctx, getState())
        enterRule(_localctx, 0, ruleIndex)
        try {
            enterOuterAlt(_localctx, 1)
            run {
                setState(12)
                statement()
                setState(13)
                match(EOF)
            }
        } catch (re: RecognitionException) {
            _localctx.exception = re
            _errHandler.reportError(this, re)
            _errHandler.recover(this, re)
        } finally {
            exitRule()
        }
        return _localctx
    }

    class StatementContext(parent: ParserRuleContext?, invokingState: Int) : ParserRuleContext(parent, invokingState) {
        fun command(): CommandContext {
            return getRuleContext(CommandContext::class.java, 0)
        }

        fun question(): QuestionContext {
            return getRuleContext(QuestionContext::class.java, 0)
        }

        fun declaration(): DeclarationContext {
            return getRuleContext(DeclarationContext::class.java, 0)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitStatement(this) else visitor.visitChildren(
                this
            )
        }
    }

    @Throws(RecognitionException::class)
    fun statement(): StatementContext {
        val _localctx = StatementContext(_ctx, getState())
        enterRule(_localctx, 2, ruleIndex)
        try {
            setState(18)
            _errHandler.sync(this)
            when (getInterpreter().adaptivePredict(_input, 0, _ctx)) {
                1 -> {
                    enterOuterAlt(_localctx, 1)
                    run {
                        setState(15)
                        command()
                    }
                }

                2 -> {
                    enterOuterAlt(_localctx, 2)
                    run {
                        setState(16)
                        question()
                    }
                }

                3 -> {
                    enterOuterAlt(_localctx, 3)
                    run {
                        setState(17)
                        declaration()
                    }
                }
            }
        } catch (re: RecognitionException) {
            _localctx.exception = re
            _errHandler.reportError(this, re)
            _errHandler.recover(this, re)
        } finally {
            exitRule()
        }
        return _localctx
    }

    open class CommandContext : ParserRuleContext {
        constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {}
        constructor() {}

        fun copyFrom(ctx: CommandContext?) {
            super.copyFrom(ctx)
        }
    }

    class HandleBulkPropertyRequestContext(ctx: CommandContext?) : CommandContext() {
        fun List(): TerminalNode {
            return getToken(List, 0)
        }

        fun Of(): TerminalNode {
            return getToken(Of, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Limits(): TerminalNode {
            return getToken(Limits, 0)
        }

        fun Goals(): TerminalNode {
            return getToken(Goals, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitHandleBulkPropertyRequest(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class InitializeJointsContext(ctx: CommandContext?) : CommandContext() {
        fun Initialize(): TerminalNode {
            return getToken(Initialize, 0)
        }

        fun Motors(): TerminalNode {
            return getToken(Motors, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitInitializeJoints(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class HandleArbitraryCommandContext(ctx: CommandContext?) : CommandContext() {
        fun phrase(): PhraseContext {
            return getRuleContext(PhraseContext::class.java, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitHandleArbitraryCommand(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class SetMotorPropertyContext(ctx: CommandContext?) : CommandContext() {
        fun Set(): TerminalNode {
            return getToken(Set, 0)
        }

        fun Property(): TerminalNode {
            return getToken(Property, 0)
        }

        fun Of(): TerminalNode {
            return getToken(Of, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun To(): TerminalNode {
            return getToken(To, 0)
        }

        fun Value(): TerminalNode {
            return getToken(Value, 0)
        }

        fun On(): TerminalNode {
            return getToken(On, 0)
        }

        fun Off(): TerminalNode {
            return getToken(Off, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        fun Unit(): TerminalNode {
            return getToken(Unit, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitSetMotorProperty(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class MoveMotorContext(ctx: CommandContext?) : CommandContext() {
        fun Move(): TerminalNode {
            return getToken(Move, 0)
        }

        fun Value(): TerminalNode {
            return getToken(Value, 0)
        }

        fun It(): TerminalNode {
            return getToken(It, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun To(): TerminalNode {
            return getToken(To, 0)
        }

        fun Unit(): TerminalNode {
            return getToken(Unit, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMoveMotor(this) else visitor.visitChildren(
                this
            )
        }
    }

    class HandleListCommand2Context(ctx: CommandContext?) : CommandContext() {
        fun List(): TerminalNode {
            return getToken(List, 0)
        }

        fun Properties(): TerminalNode {
            return getToken(Properties, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Controller(): TerminalNode {
            return getToken(Controller, 0)
        }

        fun Motor(): TerminalNode {
            return getToken(Motor, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitHandleListCommand2(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class HandleListCommand1Context(ctx: CommandContext?) : CommandContext() {
        fun List(): TerminalNode {
            return getToken(List, 0)
        }

        fun Properties(): TerminalNode {
            return getToken(Properties, 0)
        }

        fun Of(): TerminalNode {
            return getToken(Of, 0)
        }

        fun Motors(): TerminalNode {
            return getToken(Motors, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Controller(): TerminalNode {
            return getToken(Controller, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitHandleListCommand1(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class MoveSpeedContext(ctx: CommandContext?) : CommandContext() {
        fun Move(): TerminalNode {
            return getToken(Move, 0)
        }

        fun Adverb(): TerminalNode {
            return getToken(Adverb, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMoveSpeed(this) else visitor.visitChildren(
                this
            )
        }
    }

    class ConfigurationRequestContext(ctx: CommandContext?) : CommandContext() {
        fun List(): TerminalNode {
            return getToken(List, 0)
        }

        fun Configuration(): TerminalNode {
            return getToken(Configuration, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitConfigurationRequest(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class EnableTorqueContext(ctx: CommandContext?) : CommandContext() {
        fun Hold(): TerminalNode {
            return getToken(Hold, 0)
        }

        fun Freeze(): TerminalNode {
            return getToken(Freeze, 0)
        }

        fun Relax(): TerminalNode {
            return getToken(Relax, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        fun It(): TerminalNode {
            return getToken(It, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Limb(): TerminalNode {
            return getToken(Limb, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitEnableTorque(this) else visitor.visitChildren(
                this
            )
        }
    }

    class SetMotorPositionContext(ctx: CommandContext?) : CommandContext() {
        fun Set(): TerminalNode {
            return getToken(Set, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Value(): TerminalNode {
            return getToken(Value, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        fun Property(): TerminalNode {
            return getToken(Property, 0)
        }

        fun To(): TerminalNode {
            return getToken(To, 0)
        }

        fun Unit(): TerminalNode {
            return getToken(Unit, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitSetMotorPosition(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class StraightenJointContext(ctx: CommandContext?) : CommandContext() {
        fun Straighten(): TerminalNode {
            return getToken(Straighten, 0)
        }

        fun It(): TerminalNode {
            return getToken(It, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitStraightenJoint(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class HandleGreetingContext(ctx: CommandContext?) : CommandContext() {
        fun Greeting(): TerminalNode {
            return getToken(Greeting, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitHandleGreeting(
                this
            ) else visitor.visitChildren(this)
        }
    }

    @Throws(RecognitionException::class)
    fun command(): CommandContext {
        var _localctx = CommandContext(_ctx, getState())
        enterRule(_localctx, 4, ruleIndex)
        var _la: Int
        try {
            setState(203)
            _errHandler.sync(this)
            when (getInterpreter().adaptivePredict(_input, 51, _ctx)) {
                1 -> {
                    _localctx = HandleGreetingContext(_localctx)
                    enterOuterAlt(_localctx, 1)
                    run {
                        setState(20)
                        match(Greeting)
                        setState(22)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(21)
                                match(Salutation)
                            }
                        }
                    }
                }

                2 -> {
                    _localctx = InitializeJointsContext(_localctx)
                    enterOuterAlt(_localctx, 2)
                    run {
                        setState(25)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(24)
                                match(Salutation)
                            }
                        }
                        setState(27)
                        match(Initialize)
                        setState(29)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(28)
                                match(Article)
                            }
                        }
                        setState(31)
                        match(Motors)
                    }
                }

                3 -> {
                    _localctx = ConfigurationRequestContext(_localctx)
                    enterOuterAlt(_localctx, 3)
                    run {
                        setState(33)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(32)
                                match(Salutation)
                            }
                        }
                        setState(35)
                        match(List)
                        setState(37)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(36)
                                match(Article)
                            }
                        }
                        setState(39)
                        match(Configuration)
                    }
                }

                4 -> {
                    _localctx = HandleBulkPropertyRequestContext(_localctx)
                    enterOuterAlt(_localctx, 4)
                    run {
                        setState(41)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(40)
                                match(Salutation)
                            }
                        }
                        setState(43)
                        match(List)
                        setState(45)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(44)
                                match(Article)
                            }
                        }
                        setState(47)
                        _la = _input.LA(1)
                        if (!(_la == Goals || _la == Limits)) {
                            _errHandler.recoverInline(this)
                        } else {
                            if (_input.LA(1) === Token.EOF) matchedEOF = true
                            _errHandler.reportMatch(this)
                            consume()
                        }
                        setState(48)
                        match(Of)
                        setState(50)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(49)
                                match(Article)
                            }
                        }
                        setState(53)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(52)
                                match(Side)
                            }
                        }
                        setState(55)
                        match(Joint)
                        setState(57)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(56)
                                match(Axis)
                            }
                        }
                    }
                }

                5 -> {
                    _localctx = HandleListCommand1Context(_localctx)
                    enterOuterAlt(_localctx, 5)
                    run {
                        setState(60)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(59)
                                match(Salutation)
                            }
                        }
                        setState(62)
                        match(List)
                        setState(64)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(63)
                                match(Article)
                            }
                        }
                        setState(66)
                        match(Properties)
                        setState(67)
                        match(Of)
                        setState(69)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(68)
                                match(Article)
                            }
                        }
                        setState(72)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Controller) {
                            run {
                                setState(71)
                                match(Controller)
                            }
                        }
                        setState(74)
                        match(Motors)
                    }
                }

                6 -> {
                    _localctx = HandleListCommand2Context(_localctx)
                    enterOuterAlt(_localctx, 6)
                    run {
                        setState(76)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(75)
                                match(Salutation)
                            }
                        }
                        setState(78)
                        match(List)
                        setState(80)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(79)
                                match(Article)
                            }
                        }
                        setState(83)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Controller) {
                            run {
                                setState(82)
                                match(Controller)
                            }
                        }
                        setState(86)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Motor) {
                            run {
                                setState(85)
                                match(Motor)
                            }
                        }
                        setState(88)
                        match(Properties)
                    }
                }

                7 -> {
                    _localctx = MoveMotorContext(_localctx)
                    enterOuterAlt(_localctx, 7)
                    run {
                        setState(90)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(89)
                                match(Salutation)
                            }
                        }
                        setState(92)
                        match(Move)
                        setState(104)
                        _errHandler.sync(this)
                        when (_input.LA(1)) {
                            It -> {
                                setState(93)
                                match(It)
                            }

                            Article, Joint, Side -> {
                                setState(95)
                                _errHandler.sync(this)
                                _la = _input.LA(1)
                                if (_la == Article) {
                                    run {
                                        setState(94)
                                        match(Article)
                                    }
                                }
                                setState(98)
                                _errHandler.sync(this)
                                _la = _input.LA(1)
                                if (_la == Side) {
                                    run {
                                        setState(97)
                                        match(Side)
                                    }
                                }
                                setState(100)
                                match(Joint)
                                setState(102)
                                _errHandler.sync(this)
                                _la = _input.LA(1)
                                if (_la == Axis) {
                                    run {
                                        setState(101)
                                        match(Axis)
                                    }
                                }
                            }

                            else -> throw NoViableAltException(this)
                        }
                        setState(107)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == To) {
                            run {
                                setState(106)
                                match(To)
                            }
                        }
                        setState(109)
                        match(Value)
                        setState(111)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Unit) {
                            run {
                                setState(110)
                                match(Unit)
                            }
                        }
                    }
                }

                8 -> {
                    _localctx = MoveSpeedContext(_localctx)
                    enterOuterAlt(_localctx, 8)
                    run {
                        setState(114)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(113)
                                match(Salutation)
                            }
                        }
                        setState(116)
                        match(Move)
                        setState(117)
                        match(Adverb)
                    }
                }

                9 -> {
                    _localctx = EnableTorqueContext(_localctx)
                    enterOuterAlt(_localctx, 9)
                    run {
                        setState(119)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(118)
                                match(Salutation)
                            }
                        }
                        setState(121)
                        _la = _input.LA(1)
                        if (!(_la and 0x3f.inv() == 0 && 1L shl _la and (1L shl Freeze or (1L shl Relax) or (1L shl Hold)) != 0L)) {
                            _errHandler.recoverInline(this)
                        } else {
                            if (_input.LA(1) === Token.EOF) matchedEOF = true
                            _errHandler.reportMatch(this)
                            consume()
                        }
                        setState(123)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(122)
                                match(Article)
                            }
                        }
                        setState(126)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(125)
                                match(Side)
                            }
                        }
                        setState(129)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la and 0x3f.inv() == 0 && 1L shl _la and (1L shl It or (1L shl Limb) or (1L shl Joint)) != 0L) {
                            run {
                                setState(128)
                                _la = _input.LA(1)
                                if (!(_la and 0x3f.inv() == 0 && 1L shl _la and (1L shl It or (1L shl Limb) or (1L shl Joint)) != 0L)) {
                                    _errHandler.recoverInline(this)
                                } else {
                                    if (_input.LA(1) === Token.EOF) matchedEOF = true
                                    _errHandler.reportMatch(this)
                                    consume()
                                }
                            }
                        }
                        setState(132)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(131)
                                match(Axis)
                            }
                        }
                    }
                }

                10 -> {
                    _localctx = SetMotorPositionContext(_localctx)
                    enterOuterAlt(_localctx, 10)
                    run {
                        setState(135)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(134)
                                match(Salutation)
                            }
                        }
                        setState(137)
                        match(Set)
                        setState(139)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(138)
                                match(Article)
                            }
                        }
                        setState(142)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(141)
                                match(Side)
                            }
                        }
                        setState(144)
                        match(Joint)
                        setState(146)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(145)
                                match(Axis)
                            }
                        }
                        setState(149)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Property) {
                            run {
                                setState(148)
                                match(Property)
                            }
                        }
                        setState(152)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == To) {
                            run {
                                setState(151)
                                match(To)
                            }
                        }
                        setState(154)
                        match(Value)
                        setState(156)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Unit) {
                            run {
                                setState(155)
                                match(Unit)
                            }
                        }
                    }
                }

                11 -> {
                    _localctx = SetMotorPropertyContext(_localctx)
                    enterOuterAlt(_localctx, 11)
                    run {
                        setState(159)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(158)
                                match(Salutation)
                            }
                        }
                        setState(161)
                        match(Set)
                        setState(163)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(162)
                                match(Article)
                            }
                        }
                        setState(165)
                        match(Property)
                        setState(166)
                        match(Of)
                        setState(168)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(167)
                                match(Article)
                            }
                        }
                        setState(171)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(170)
                                match(Side)
                            }
                        }
                        setState(173)
                        match(Joint)
                        setState(175)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(174)
                                match(Axis)
                            }
                        }
                        setState(177)
                        match(To)
                        setState(178)
                        _la = _input.LA(1)
                        if (!(_la and 0x3f.inv() == 0 && 1L shl _la and (1L shl Off or (1L shl On) or (1L shl Value)) != 0L)) {
                            _errHandler.recoverInline(this)
                        } else {
                            if (_input.LA(1) === Token.EOF) matchedEOF = true
                            _errHandler.reportMatch(this)
                            consume()
                        }
                        setState(180)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Unit) {
                            run {
                                setState(179)
                                match(Unit)
                            }
                        }
                    }
                }

                12 -> {
                    _localctx = StraightenJointContext(_localctx)
                    enterOuterAlt(_localctx, 12)
                    run {
                        setState(183)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(182)
                                match(Salutation)
                            }
                        }
                        setState(185)
                        match(Straighten)
                        setState(197)
                        _errHandler.sync(this)
                        when (_input.LA(1)) {
                            It -> {
                                setState(186)
                                match(It)
                            }

                            Article, Joint, Side -> {
                                setState(188)
                                _errHandler.sync(this)
                                _la = _input.LA(1)
                                if (_la == Article) {
                                    run {
                                        setState(187)
                                        match(Article)
                                    }
                                }
                                setState(191)
                                _errHandler.sync(this)
                                _la = _input.LA(1)
                                if (_la == Side) {
                                    run {
                                        setState(190)
                                        match(Side)
                                    }
                                }
                                setState(193)
                                match(Joint)
                                setState(195)
                                _errHandler.sync(this)
                                _la = _input.LA(1)
                                if (_la == Axis) {
                                    run {
                                        setState(194)
                                        match(Axis)
                                    }
                                }
                            }

                            else -> throw NoViableAltException(this)
                        }
                    }
                }

                13 -> {
                    _localctx = HandleArbitraryCommandContext(_localctx)
                    enterOuterAlt(_localctx, 13)
                    run {
                        setState(200)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(199)
                                match(Salutation)
                            }
                        }
                        setState(202)
                        phrase()
                    }
                }
            }
        } catch (re: RecognitionException) {
            _localctx.exception = re
            _errHandler.reportError(this, re)
            _errHandler.recover(this, re)
        } finally {
            exitRule()
        }
        return _localctx
    }

    open class QuestionContext : ParserRuleContext {
        constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {}
        constructor() {}

        fun copyFrom(ctx: QuestionContext?) {
            super.copyFrom(ctx)
        }
    }

    class MotorPropertyQuestion1Context(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Property(): TerminalNode {
            return getToken(Property, 0)
        }

        fun Of(): TerminalNode {
            return getToken(Of, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMotorPropertyQuestion1(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class ConfigurationQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Configuration(): TerminalNode {
            return getToken(Configuration, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitConfigurationQuestion(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class MotorPropertyQuestion2Context(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Property(): TerminalNode {
            return getToken(Property, 0)
        }

        fun Of(): TerminalNode {
            return getToken(Of, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMotorPropertyQuestion2(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class WhyMittensContext(ctx: QuestionContext?) : QuestionContext() {
        fun Why(): TerminalNode {
            return getToken(Why, 0)
        }

        fun Do(): TerminalNode {
            return getToken(Do, 0)
        }

        fun You(): TerminalNode {
            return getToken(You, 0)
        }

        fun Have(): TerminalNode {
            return getToken(Have, 0)
        }

        fun Mittens(): TerminalNode {
            return getToken(Mittens, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitWhyMittens(this) else visitor.visitChildren(
                this
            )
        }
    }

    class JointPropertyQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Property(): TerminalNode {
            return getToken(Property, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitJointPropertyQuestion(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class PoseQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Pose(): TerminalNode {
            return getToken(Pose, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Adjective(): TerminalNode {
            return getToken(Adjective, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitPoseQuestion(this) else visitor.visitChildren(
                this
            )
        }
    }

    class MetricsQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Metric(): TerminalNode {
            return getToken(Metric, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMetricsQuestion(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class LimbLocationQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun Where(): TerminalNode {
            return getToken(Where, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Appendage(): TerminalNode {
            return getToken(Appendage, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitLimbLocationQuestion(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class HandleBulkPropertyQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun What(): TerminalNode {
            return getToken(What, 0)
        }

        fun Are(): TerminalNode {
            return getToken(Are, 0)
        }

        fun Of(): TerminalNode {
            return getToken(Of, 0)
        }

        fun Joint(): TerminalNode {
            return getToken(Joint, 0)
        }

        fun Limits(): TerminalNode {
            return getToken(Limits, 0)
        }

        fun Goals(): TerminalNode {
            return getToken(Goals, 0)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Side(): TerminalNode {
            return getToken(Side, 0)
        }

        fun Axis(): TerminalNode {
            return getToken(Axis, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitHandleBulkPropertyQuestion(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class AttributeQuestionContext(ctx: QuestionContext?) : QuestionContext() {
        fun How(): TerminalNode {
            return getToken(How, 0)
        }

        fun Attribute(): TerminalNode {
            return getToken(Attribute, 0)
        }

        fun Are(): TerminalNode {
            return getToken(Are, 0)
        }

        fun You(): TerminalNode {
            return getToken(You, 0)
        }

        fun Salutation(): TerminalNode {
            return getToken(Salutation, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitAttributeQuestion(
                this
            ) else visitor.visitChildren(this)
        }
    }

    @Throws(RecognitionException::class)
    fun question(): QuestionContext {
        var _localctx = QuestionContext(_ctx, getState())
        enterRule(_localctx, 6, ruleIndex)
        var _la: Int
        try {
            setState(314)
            _errHandler.sync(this)
            when (getInterpreter().adaptivePredict(_input, 75, _ctx)) {
                1 -> {
                    _localctx = AttributeQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 1)
                    run {
                        setState(206)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Salutation) {
                            run {
                                setState(205)
                                match(Salutation)
                            }
                        }
                        setState(208)
                        match(How)
                        setState(209)
                        match(Attribute)
                        setState(210)
                        match(Are)
                        setState(211)
                        match(You)
                    }
                }

                2 -> {
                    _localctx = ConfigurationQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 2)
                    run {
                        setState(212)
                        match(What)
                        setState(213)
                        match(Is)
                        setState(215)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(214)
                                match(Article)
                            }
                        }
                        setState(217)
                        match(Configuration)
                    }
                }

                3 -> {
                    _localctx = HandleBulkPropertyQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 3)
                    run {
                        setState(218)
                        match(What)
                        setState(219)
                        match(Are)
                        setState(221)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(220)
                                match(Article)
                            }
                        }
                        setState(223)
                        _la = _input.LA(1)
                        if (!(_la == Goals || _la == Limits)) {
                            _errHandler.recoverInline(this)
                        } else {
                            if (_input.LA(1) === Token.EOF) matchedEOF = true
                            _errHandler.reportMatch(this)
                            consume()
                        }
                        setState(224)
                        match(Of)
                        setState(226)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(225)
                                match(Article)
                            }
                        }
                        setState(229)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(228)
                                match(Side)
                            }
                        }
                        setState(231)
                        match(Joint)
                        setState(233)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(232)
                                match(Axis)
                            }
                        }
                    }
                }

                4 -> {
                    _localctx = JointPropertyQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 4)
                    run {
                        setState(235)
                        match(What)
                        setState(236)
                        match(Is)
                        setState(238)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(237)
                                match(Article)
                            }
                        }
                        setState(241)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(240)
                                match(Side)
                            }
                        }
                        setState(243)
                        match(Joint)
                        setState(245)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(244)
                                match(Axis)
                            }
                        }
                        setState(247)
                        match(Property)
                    }
                }

                5 -> {
                    _localctx = MotorPropertyQuestion1Context(_localctx)
                    enterOuterAlt(_localctx, 5)
                    run {
                        setState(248)
                        match(What)
                        setState(249)
                        match(Is)
                        setState(251)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(250)
                                match(Article)
                            }
                        }
                        setState(254)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(253)
                                match(Axis)
                            }
                        }
                        setState(256)
                        match(Property)
                        setState(257)
                        match(Of)
                        setState(259)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(258)
                                match(Article)
                            }
                        }
                        setState(262)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(261)
                                match(Side)
                            }
                        }
                        setState(264)
                        match(Joint)
                    }
                }

                6 -> {
                    _localctx = MotorPropertyQuestion2Context(_localctx)
                    enterOuterAlt(_localctx, 6)
                    run {
                        setState(265)
                        match(What)
                        setState(266)
                        match(Is)
                        setState(268)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(267)
                                match(Article)
                            }
                        }
                        setState(270)
                        match(Property)
                        setState(271)
                        match(Of)
                        setState(273)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(272)
                                match(Article)
                            }
                        }
                        setState(276)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(275)
                                match(Side)
                            }
                        }
                        setState(278)
                        match(Joint)
                        setState(280)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(279)
                                match(Axis)
                            }
                        }
                    }
                }

                7 -> {
                    _localctx = MetricsQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 7)
                    run {
                        setState(282)
                        match(What)
                        setState(283)
                        match(Is)
                        setState(285)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(284)
                                match(Article)
                            }
                        }
                        setState(287)
                        match(Metric)
                    }
                }

                8 -> {
                    _localctx = PoseQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 8)
                    run {
                        setState(288)
                        match(What)
                        setState(289)
                        match(Is)
                        setState(291)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(290)
                                match(Article)
                            }
                        }
                        setState(294)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Adjective) {
                            run {
                                setState(293)
                                match(Adjective)
                            }
                        }
                        setState(296)
                        match(Pose)
                    }
                }

                9 -> {
                    _localctx = LimbLocationQuestionContext(_localctx)
                    enterOuterAlt(_localctx, 9)
                    run {
                        setState(297)
                        match(Where)
                        setState(298)
                        match(Is)
                        setState(300)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(299)
                                match(Article)
                            }
                        }
                        setState(303)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Side) {
                            run {
                                setState(302)
                                match(Side)
                            }
                        }
                        setState(305)
                        _la = _input.LA(1)
                        if (!(_la == Appendage || _la == Joint)) {
                            _errHandler.recoverInline(this)
                        } else {
                            if (_input.LA(1) === Token.EOF) matchedEOF = true
                            _errHandler.reportMatch(this)
                            consume()
                        }
                        setState(307)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Axis) {
                            run {
                                setState(306)
                                match(Axis)
                            }
                        }
                    }
                }

                10 -> {
                    _localctx = WhyMittensContext(_localctx)
                    enterOuterAlt(_localctx, 10)
                    run {
                        setState(309)
                        match(Why)
                        setState(310)
                        match(Do)
                        setState(311)
                        match(You)
                        setState(312)
                        match(Have)
                        setState(313)
                        match(Mittens)
                    }
                }
            }
        } catch (re: RecognitionException) {
            _localctx.exception = re
            _errHandler.reportError(this, re)
            _errHandler.recover(this, re)
        } finally {
            exitRule()
        }
        return _localctx
    }

    open class DeclarationContext : ParserRuleContext {
        constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {}
        constructor() {}

        fun copyFrom(ctx: DeclarationContext?) {
            super.copyFrom(ctx)
        }
    }

    class MapPoseToCommand5Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun When(): TerminalNode {
            return getToken(When, 0)
        }

        fun You(): List<TerminalNode> {
            return getTokens(You)
        }

        fun You(i: Int): TerminalNode {
            return getToken(You, i)
        }

        fun phrase(): List<PhraseContext> {
            return getRuleContexts(PhraseContext::class.java)
        }

        fun phrase(i: Int): PhraseContext {
            return getRuleContext(PhraseContext::class.java, i)
        }

        fun Then(): TerminalNode {
            return getToken(Then, 0)
        }

        fun Are(): TerminalNode {
            return getToken(Are, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMapPoseToCommand5(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class MapPoseToCommand4Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun When(): TerminalNode {
            return getToken(When, 0)
        }

        fun Isay(): TerminalNode {
            return getToken(Isay, 0)
        }

        fun phrase(): List<PhraseContext> {
            return getRuleContexts(PhraseContext::class.java)
        }

        fun phrase(i: Int): PhraseContext {
            return getRuleContext(PhraseContext::class.java, i)
        }

        fun Take(): TerminalNode {
            return getToken(Take, 0)
        }

        fun Pose(): TerminalNode {
            return getToken(Pose, 0)
        }

        fun Then(): TerminalNode {
            return getToken(Then, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMapPoseToCommand4(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class MapPoseToCommand3Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun To(): List<TerminalNode> {
            return getTokens(To)
        }

        fun To(i: Int): TerminalNode {
            return getToken(To, i)
        }

        fun phrase(): List<PhraseContext> {
            return getRuleContexts(PhraseContext::class.java)
        }

        fun phrase(i: Int): PhraseContext {
            return getRuleContext(PhraseContext::class.java, i)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun Be(): TerminalNode {
            return getToken(Be, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMapPoseToCommand3(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class DeclarePose1Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun You(): TerminalNode {
            return getToken(You, 0)
        }

        fun Are(): TerminalNode {
            return getToken(Are, 0)
        }

        fun phrase(): PhraseContext {
            return getRuleContext(PhraseContext::class.java, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitDeclarePose1(this) else visitor.visitChildren(
                this
            )
        }
    }

    class DeclarePose2Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun Pose(): TerminalNode {
            return getToken(Pose, 0)
        }

        fun Is(): TerminalNode {
            return getToken(Is, 0)
        }

        fun phrase(): PhraseContext {
            return getRuleContext(PhraseContext::class.java, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitDeclarePose2(this) else visitor.visitChildren(
                this
            )
        }
    }

    class MapPoseToCommand2Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun To(): TerminalNode {
            return getToken(To, 0)
        }

        fun phrase(): List<PhraseContext> {
            return getRuleContexts(PhraseContext::class.java)
        }

        fun phrase(i: Int): PhraseContext {
            return getRuleContext(PhraseContext::class.java, i)
        }

        fun Means(): TerminalNode {
            return getToken(Means, 0)
        }

        fun You(): TerminalNode {
            return getToken(You, 0)
        }

        fun Are(): TerminalNode {
            return getToken(Are, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMapPoseToCommand2(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class MapPoseToCommand1Context(ctx: DeclarationContext?) : DeclarationContext() {
        fun To(): List<TerminalNode> {
            return getTokens(To)
        }

        fun To(i: Int): TerminalNode {
            return getToken(To, i)
        }

        fun phrase(): List<PhraseContext> {
            return getRuleContexts(PhraseContext::class.java)
        }

        fun phrase(i: Int): PhraseContext {
            return getRuleContext(PhraseContext::class.java, i)
        }

        fun Means(): TerminalNode {
            return getToken(Means, 0)
        }

        fun Take(): TerminalNode {
            return getToken(Take, 0)
        }

        fun Pose(): TerminalNode {
            return getToken(Pose, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitMapPoseToCommand1(
                this
            ) else visitor.visitChildren(this)
        }
    }

    class DeclareNoNamePoseContext(ctx: DeclarationContext?) : DeclarationContext() {
        fun Save(): TerminalNode {
            return getToken(Save, 0)
        }

        fun Pose(): TerminalNode {
            return getToken(Pose, 0)
        }

        fun Article(): TerminalNode {
            return getToken(Article, 0)
        }

        fun As(): TerminalNode {
            return getToken(As, 0)
        }

        fun phrase(): PhraseContext {
            return getRuleContext(PhraseContext::class.java, 0)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitDeclareNoNamePose(
                this
            ) else visitor.visitChildren(this)
        }
    }

    @Throws(RecognitionException::class)
    fun declaration(): DeclarationContext {
        var _localctx = DeclarationContext(_ctx, getState())
        enterRule(_localctx, 8, ruleIndex)
        var _la: Int
        try {
            setState(378)
            _errHandler.sync(this)
            when (getInterpreter().adaptivePredict(_input, 81, _ctx)) {
                1 -> {
                    _localctx = DeclarePose1Context(_localctx)
                    enterOuterAlt(_localctx, 1)
                    run {
                        setState(316)
                        match(You)
                        setState(317)
                        match(Are)
                        setState(318)
                        phrase()
                    }
                }

                2 -> {
                    _localctx = DeclarePose2Context(_localctx)
                    enterOuterAlt(_localctx, 2)
                    run {
                        setState(319)
                        match(Article)
                        setState(320)
                        match(Pose)
                        setState(321)
                        match(Is)
                        setState(322)
                        phrase()
                    }
                }

                3 -> {
                    _localctx = DeclareNoNamePoseContext(_localctx)
                    enterOuterAlt(_localctx, 3)
                    run {
                        setState(323)
                        match(Save)
                        setState(325)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(324)
                                match(Article)
                            }
                        }
                        setState(327)
                        match(Pose)
                        setState(330)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == As) {
                            run {
                                setState(328)
                                match(As)
                                setState(329)
                                phrase()
                            }
                        }
                    }
                }

                4 -> {
                    _localctx = MapPoseToCommand1Context(_localctx)
                    enterOuterAlt(_localctx, 4)
                    run {
                        setState(332)
                        match(To)
                        setState(333)
                        phrase()
                        setState(334)
                        match(Means)
                        setState(335)
                        match(To)
                        setState(336)
                        match(Take)
                        setState(338)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(337)
                                match(Article)
                            }
                        }
                        setState(340)
                        match(Pose)
                        setState(341)
                        phrase()
                    }
                }

                5 -> {
                    _localctx = MapPoseToCommand2Context(_localctx)
                    enterOuterAlt(_localctx, 5)
                    run {
                        setState(343)
                        match(To)
                        setState(344)
                        phrase()
                        setState(345)
                        match(Means)
                        setState(346)
                        match(You)
                        setState(347)
                        match(Are)
                        setState(348)
                        phrase()
                    }
                }

                6 -> {
                    _localctx = MapPoseToCommand3Context(_localctx)
                    enterOuterAlt(_localctx, 6)
                    run {
                        setState(350)
                        match(To)
                        setState(351)
                        phrase()
                        setState(352)
                        match(Is)
                        setState(353)
                        match(To)
                        setState(354)
                        match(Be)
                        setState(355)
                        phrase()
                    }
                }

                7 -> {
                    _localctx = MapPoseToCommand4Context(_localctx)
                    enterOuterAlt(_localctx, 7)
                    run {
                        setState(357)
                        match(When)
                        setState(358)
                        match(Isay)
                        setState(359)
                        phrase()
                        setState(361)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Then) {
                            run {
                                setState(360)
                                match(Then)
                            }
                        }
                        setState(363)
                        match(Take)
                        setState(365)
                        _errHandler.sync(this)
                        _la = _input.LA(1)
                        if (_la == Article) {
                            run {
                                setState(364)
                                match(Article)
                            }
                        }
                        setState(367)
                        match(Pose)
                        setState(368)
                        phrase()
                    }
                }

                8 -> {
                    _localctx = MapPoseToCommand5Context(_localctx)
                    enterOuterAlt(_localctx, 8)
                    run {
                        setState(370)
                        match(When)
                        setState(371)
                        match(You)
                        setState(372)
                        phrase()
                        setState(373)
                        match(Then)
                        setState(374)
                        match(You)
                        setState(375)
                        match(Are)
                        setState(376)
                        phrase()
                    }
                }
            }
        } catch (re: RecognitionException) {
            _localctx.exception = re
            _errHandler.reportError(this, re)
            _errHandler.recover(this, re)
        } finally {
            exitRule()
        }
        return _localctx
    }

    open class PhraseContext : ParserRuleContext {
        constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {}
        constructor() {}

        fun copyFrom(ctx: PhraseContext?) {
            super.copyFrom(ctx)
        }
    }

    class WordListContext(ctx: PhraseContext?) : PhraseContext() {
        fun NAME(): List<TerminalNode> {
            return getTokens(NAME)
        }

        fun NAME(i: Int): TerminalNode {
            return getToken(NAME, i)
        }

        fun Value(): List<TerminalNode> {
            return getTokens(Value)
        }

        fun Value(i: Int): TerminalNode {
            return getToken(Value, i)
        }

        fun Appendage(): List<TerminalNode> {
            return getTokens(Appendage)
        }

        fun Appendage(i: Int): TerminalNode {
            return getToken(Appendage, i)
        }

        fun Are(): List<TerminalNode> {
            return getTokens(Are)
        }

        fun Are(i: Int): TerminalNode {
            return getToken(Are, i)
        }

        fun As(): List<TerminalNode> {
            return getTokens(As)
        }

        fun As(i: Int): TerminalNode {
            return getToken(As, i)
        }

        fun Article(): List<TerminalNode> {
            return getTokens(Article)
        }

        fun Article(i: Int): TerminalNode {
            return getToken(Article, i)
        }

        fun Axis(): List<TerminalNode> {
            return getTokens(Axis)
        }

        fun Axis(i: Int): TerminalNode {
            return getToken(Axis, i)
        }

        fun Freeze(): List<TerminalNode> {
            return getTokens(Freeze)
        }

        fun Freeze(i: Int): TerminalNode {
            return getToken(Freeze, i)
        }

        fun Hold(): List<TerminalNode> {
            return getTokens(Hold)
        }

        fun Hold(i: Int): TerminalNode {
            return getToken(Hold, i)
        }

        fun It(): List<TerminalNode> {
            return getTokens(It)
        }

        fun It(i: Int): TerminalNode {
            return getToken(It, i)
        }

        fun Joint(): List<TerminalNode> {
            return getTokens(Joint)
        }

        fun Joint(i: Int): TerminalNode {
            return getToken(Joint, i)
        }

        fun Move(): List<TerminalNode> {
            return getTokens(Move)
        }

        fun Move(i: Int): TerminalNode {
            return getToken(Move, i)
        }

        fun Of(): List<TerminalNode> {
            return getTokens(Of)
        }

        fun Of(i: Int): TerminalNode {
            return getToken(Of, i)
        }

        fun Relax(): List<TerminalNode> {
            return getTokens(Relax)
        }

        fun Relax(i: Int): TerminalNode {
            return getToken(Relax, i)
        }

        fun Set(): List<TerminalNode> {
            return getTokens(Set)
        }

        fun Set(i: Int): TerminalNode {
            return getToken(Set, i)
        }

        fun Side(): List<TerminalNode> {
            return getTokens(Side)
        }

        fun Side(i: Int): TerminalNode {
            return getToken(Side, i)
        }

        fun Straighten(): List<TerminalNode> {
            return getTokens(Straighten)
        }

        fun Straighten(i: Int): TerminalNode {
            return getToken(Straighten, i)
        }

        fun Take(): List<TerminalNode> {
            return getTokens(Take)
        }

        fun Take(i: Int): TerminalNode {
            return getToken(Take, i)
        }

        fun To(): List<TerminalNode> {
            return getTokens(To)
        }

        fun To(i: Int): TerminalNode {
            return getToken(To, i)
        }

        init {
            copyFrom(ctx)
        }

        fun <T> accept(visitor: ParseTreeVisitor<out T>): T? {
            return if (visitor is SpeechSyntaxVisitor<*>) (visitor as SpeechSyntaxVisitor<out T>).visitWordList(this) else visitor.visitChildren(
                this
            )
        }
    }

    @Throws(RecognitionException::class)
    fun phrase(): PhraseContext {
        var _localctx = PhraseContext(_ctx, getState())
        enterRule(_localctx, 10, ruleIndex)
        var _la: Int
        try {
            var _alt: Int
            _localctx = WordListContext(_localctx)
            enterOuterAlt(_localctx, 1)
            run {
                setState(381)
                _errHandler.sync(this)
                _alt = 1
                do {
                    when (_alt) {
                        1 -> {
                            run {
                                setState(380)
                                _la = _input.LA(1)
                                if (!(_la and 0x3f.inv() == 0 && 1L shl _la and (1L shl Freeze or (1L shl Relax) or (1L shl Article) or (1L shl Appendage) or (1L shl Are) or (1L shl As) or (1L shl Axis) or (1L shl Hold) or (1L shl It) or (1L shl Move) or (1L shl Of) or (1L shl Joint) or (1L shl Set) or (1L shl Side) or (1L shl Straighten) or (1L shl Take) or (1L shl To) or (1L shl Value) or (1L shl NAME)) != 0L)) {
                                    _errHandler.recoverInline(this)
                                } else {
                                    if (_input.LA(1) === Token.EOF) matchedEOF = true
                                    _errHandler.reportMatch(this)
                                    consume()
                                }
                            }
                        }

                        else -> throw NoViableAltException(this)
                    }
                    setState(383)
                    _errHandler.sync(this)
                    _alt = getInterpreter().adaptivePredict(_input, 82, _ctx)
                } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER)
            }
        } catch (re: RecognitionException) {
            _localctx.exception = re
            _errHandler.reportError(this, re)
            _errHandler.recover(this, re)
        } finally {
            exitRule()
        }
        return _localctx
    }

    init {
        _interp = ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache)
    }

    companion object {
        init {
            RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION)
        }

        protected val _decisionToDFA: Array<DFA?>
        protected val _sharedContextCache: PredictionContextCache = PredictionContextCache()
        const val Freeze = 1
        const val Relax = 2
        const val Why = 3
        const val Article = 4
        const val Adjective = 5
        const val Adverb = 6
        const val Appendage = 7
        const val Are = 8
        const val As = 9
        const val Attribute = 10
        const val Axis = 11
        const val Be = 12
        const val Configuration = 13
        const val Controller = 14
        const val Do = 15
        const val Goals = 16
        const val Greeting = 17
        const val Have = 18
        const val Hold = 19
        const val How = 20
        const val Initialize = 21
        const val Isay = 22
        const val Is = 23
        const val It = 24
        const val List = 25
        const val Limb = 26
        const val Limits = 27
        const val Means = 28
        const val Metric = 29
        const val Mittens = 30
        const val Motors = 31
        const val Motor = 32
        const val Move = 33
        const val Of = 34
        const val Off = 35
        const val On = 36
        const val Joint = 37
        const val Pose = 38
        const val Properties = 39
        const val Property = 40
        const val Reset = 41
        const val Salutation = 42
        const val Save = 43
        const val Set = 44
        const val Side = 45
        const val Straighten = 46
        const val Take = 47
        const val Then = 48
        const val To = 49
        const val Unit = 50
        const val Value = 51
        const val You = 52
        const val What = 53
        const val When = 54
        const val Where = 55
        const val COMMA = 56
        const val COLON = 57
        const val DECIMAL = 58
        const val INTEGER = 59
        const val NAME = 60
        const val EQUAL = 61
        const val SLASH = 62
        const val PCLOSE = 63
        const val POPEN = 64
        const val DBLQUOTE = 65
        const val SNGLQUOTE = 66
        val ruleIndex = 0
            get() = Companion.field
        val ruleIndex = 1
            get() = Companion.field
        val ruleIndex = 2
            get() = Companion.field
        val ruleIndex = 3
            get() = Companion.field
        val ruleIndex = 4
            get() = Companion.field
        val ruleIndex = 5
            get() = Companion.field

        private fun makeRuleNames(): Array<String> {
            return arrayOf(
                "line", "statement", "command", "question", "declaration", "phrase"
            )
        }

        val ruleNames = makeRuleNames()
        private fun makeLiteralNames(): Array<String?> {
            return arrayOf(
                null, null, null, "'why'", null, "'current'", null, null, "'are'", "'as'",
                null, null, null, "'configuration'", null, "'do'", null, null, null,
                "'hold'", "'how'", "'initialize'", "'i say'", "'is'", "'it'", null, null,
                "'limits'", "'means'", null, "'mittens'", null, null, null, null, "'off'",
                "'on'", null, "'pose'", null, null, "'reset'", null, "'save'", "'set'",
                null, "'straighten'", null, "'then'", "'to'", "'degrees'", null, "'you'",
                "'what'", "'when'", "'where'", "','", "':'", null, null, null, "'='",
                "'/'", "')'", "'('", "'\"'", "'''"
            )
        }

        private val _LITERAL_NAMES = makeLiteralNames()
        private fun makeSymbolicNames(): Array<String?> {
            return arrayOf(
                null, "Freeze", "Relax", "Why", "Article", "Adjective", "Adverb", "Appendage",
                "Are", "As", "Attribute", "Axis", "Be", "Configuration", "Controller",
                "Do", "Goals", "Greeting", "Have", "Hold", "How", "Initialize", "Isay",
                "Is", "It", "List", "Limb", "Limits", "Means", "Metric", "Mittens", "Motors",
                "Motor", "Move", "Of", "Off", "On", "Joint", "Pose", "Properties", "Property",
                "Reset", "Salutation", "Save", "Set", "Side", "Straighten", "Take", "Then",
                "To", "Unit", "Value", "You", "What", "When", "Where", "COMMA", "COLON",
                "DECIMAL", "INTEGER", "NAME", "EQUAL", "SLASH", "PCLOSE", "POPEN", "DBLQUOTE",
                "SNGLQUOTE"
            )
        }

        private val _SYMBOLIC_NAMES = makeSymbolicNames()
        val VOCABULARY: Vocabulary = VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES)

        @Deprecated("Use {@link #VOCABULARY} instead.")
        val tokenNames: Array<String?>

        init {
            tokenNames = arrayOfNulls(_SYMBOLIC_NAMES.size)
            for (i in tokenNames.indices) {
                tokenNames[i] = VOCABULARY.getLiteralName(i)
                if (tokenNames[i] == null) {
                    tokenNames[i] = VOCABULARY.getSymbolicName(i)
                }
                if (tokenNames[i] == null) {
                    tokenNames[i] = "<INVALID>"
                }
            }
        }

        val serializedATN = """
               悋Ꜫ脳맭䅼㯧瞆奤DƄ						
               
               
                
               $
               (
               ,
               0
               5
               8
               <
               ?
               C
               H
               K
               O
               S
               V
               Y
               ]
               b
               e
               i
               k
               n
               r
               u
               z
               ~
               
               
               
               
               
               
               
               
               
               
               ¢
               ¦
               «
               ®
               ²
               ·
               º
               ¿
               Â
               Æ
               È
               Ë
               Î
               Ñ
               Ú
               à
               å
               è
               ì
               ñ
               ô
               ø
               þ
               ā
               Ć
               ĉ
               ď
               Ĕ
               ė
               ě
               Ġ
               Ħ
               ĩ
               į
               Ĳ
               Ķ
               Ľ
               ň
               ō
               ŕ
               Ŭ
               Ű
               Ž
               ƀ
               
               Ɓ
               ''%&55		''	
               
               #$''.13355>>ǪÍļ
               żſ
               ,Î,   !!Î!"$,#"#$$%%'&('&'(())Î*,,+*+,,--/.0/./00112	24$3543455768/7678899;':<
               ;:;<<Î=?,>=>??@@BACBABCCDDE)EG$FHGFGHHJIKJIJKKLLÎ!MO,NMNOOPPRQSRQRSSUTVUTUVVXWY"XWXYYZZÎ)[],\[\]]^^j#_k`ba`abbdce/dcdeeffh'gi
               hghiikj_jakmln3mlmnnooq5pr4qpqrrÎsu,tstuuvvw#wÎxz,yxyzz{{}	|~}|}~~/	
               Î,./'
               *354Î ¢,¡ ¡¢¢££¥.¤¦¥¤¥¦¦§§¨*¨ª$©«ª©ª««­¬®/­¬­®®¯¯±'°²
               ±°±²²³³´3´¶	µ·4¶µ¶··Î¸º,¹¸¹ºº»»Ç0¼È½¿¾½¾¿¿ÁÀÂ/ÁÀÁÂÂÃÃÅ'ÄÆ
               ÅÄÅÆÆÈÇ¼Ç¾ÈÎÉË,ÊÉÊËËÌÌÎÍÍÍ#Í+Í>ÍNÍ\ÍtÍyÍÍ¡Í¹ÍÊÎÏÑ,ÐÏÐÑÑÒÒÓÓÔÔÕ
               ÕĽ6Ö×7×ÙØÚÙØÙÚÚÛÛĽÜÝ7Ýß
               ÞàßÞßààááâ	âä$ãåäãäååçæè/çæçèèééë'êì
               ëêëììĽíî7îðïñðïðññóòô/óòóôôõõ÷'öø
               ÷ö÷øøùùĽ*úû7ûýüþýüýþþĀÿā
               ĀÿĀāāĂĂă*ăą$ĄĆąĄąĆĆĈćĉ/ĈćĈĉĉĊĊĽ'ċČ7ČĎčďĎčĎďďĐĐđ*đē$ĒĔēĒēĔĔĖĕė/ĖĕĖėėĘĘĚ'ęě
               ĚęĚěěĽĜĝ7ĝğĞĠğĞğĠĠġġĽĢģ7ģĥĤĦĥĤĥĦĦĨħĩĨħĨĩĩĪĪĽ(īĬ9ĬĮĭįĮĭĮįįıİĲ/ıİıĲĲĳĳĵ	ĴĶ
               ĵĴĵĶĶĽķĸĸĹĹĺ6ĺĻĻĽ ļÐļÖļÜļíļúļċļĜļĢļīļķĽ	ľĿ6Ŀŀ
               ŀŽŁłłŃ(ŃńńŽŅŇ-ņňŇņŇňňŉŉŌ(ŊŋŋōŌŊŌōōŽŎŏ3ŏŐŐőőŒ3ŒŔ1œŕŔœŔŕŕŖŖŗ(ŗŘŘŽřŚ3ŚśśŜŜŝ6ŝŞ
               ŞşşŽŠš3šŢŢţţŤ3ŤťťŦŦŽŧŨ8ŨũũūŪŬ2ūŪūŬŬŭŭů1ŮŰůŮůŰŰűűŲ(ŲųųŽŴŵ8ŵŶ6ŶŷŷŸ2ŸŹ6Źź
               źŻŻŽżľżŁżŅżŎżřżŠżŧżŴŽžƀ	ſžƀƁƁſƁƂƂ
               U#'+/47;>BGJNRUX\adhjmqty}¡¥ª­±¶¹¾ÁÅÇÊÍÐÙßäçëðó÷ýĀąĈĎēĖĚğĥĨĮıĵļŇŌŔūůżƁ
               """.trimIndent()
            get() = Companion.field
        val _ATN: ATN = ATNDeserializer().deserialize(serializedATN.toCharArray())

        init {
            _decisionToDFA = arrayOfNulls<DFA>(_ATN.getNumberOfDecisions())
            for (i in 0 until _ATN.getNumberOfDecisions()) {
                _decisionToDFA[i] = DFA(_ATN.getDecisionState(i), i)
            }
        }
    }
}