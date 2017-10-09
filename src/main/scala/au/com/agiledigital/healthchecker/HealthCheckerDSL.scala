package au.com.agiledigital.healthchecker

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.control.NonFatal

/**
  * DSL for writing health checkers in the same style as Kanaka monadic actions.
  */
object HealthCheckerDSL {
  final case class HealthCheckStep[+A](run: Future[Either[HealthCheckOutcome, A]]) {

    def map[B](f: A => B)(implicit ec: ExecutionContext): HealthCheckStep[B] = copy(run = run.map(_.right.map(f)))

    def flatMap[B](f: A => HealthCheckStep[B])(implicit ec: ExecutionContext): HealthCheckStep[B] =
      copy(run = run.flatMap(_.fold(err => Future.successful(Left[HealthCheckOutcome, B](err)), succ => f(succ).run)))

    def withFilter(p: A => Boolean)(implicit ec: ExecutionContext): HealthCheckStep[A] =
      copy(run = run.filter {
        case Right(a) if p(a) => true
        case Left(_)          => true
        case _                => false
      })
  }

  object Step {
    def unit(outcome: HealthCheckOutcome): HealthCheckStep[HealthCheckOutcome] = HealthCheckStep(Future.successful(Right(outcome)))
  }

  trait HealthStepOps[A, B] {
    def orFailWith(failureHandler: B => HealthCheckOutcome): HealthCheckStep[A]

    def ?|(failureHandler: B => HealthCheckOutcome): HealthCheckStep[A] = orFailWith(failureHandler)

    def ?|(failureThunk: => HealthCheckOutcome): HealthCheckStep[A] = orFailWith(_ => failureThunk)
  }

  trait MonadicHealthChecks {

    /**
      * Converts a future to a step by recovering the failed future using the failure handler.
      *
      * @param future the future to convert.
      * @tparam A the type of the value produced by the future.
      * @return the step.
      */
    implicit def futureToStepOps[A](future: Future[A])(implicit ec: ExecutionContext): HealthStepOps[A, Throwable] =
      (failureHandler: (Throwable) => HealthCheckOutcome) =>
        HealthCheckStep(future.map(Right(_)).recover {
          case NonFatal(t) => Left(failureHandler(t))
        })

    /**
      * Converts an either to a step by mapping the left side using the failure handler.
      *
      * @param either the either to convert.
      * @tparam A the right side of the either.
      * @tparam B the left side of the either.
      * @return the step.
      */
    implicit def eitherToStepOps[A, B](either: Either[B, A]): HealthStepOps[A, B] = (failureHandler: (B) => HealthCheckOutcome) => HealthCheckStep(Future.successful(either.left.map(failureHandler)))

    /**
      * Converts a future either to a step by mapping the left side using the failure handler.
      *
      * @param fEither the future either to convert.
      * @tparam A the right side of the either.
      * @tparam B the left side of the either.
      * @return the step.
      */
    implicit def fEitherToStepOps[A, B](fEither: Future[Either[B, A]])(implicit ec: ExecutionContext): HealthStepOps[A, B] =
      (failureHandler: (B) => HealthCheckOutcome) => HealthCheckStep(fEither.map(_.left.map(failureHandler)))

    /**
      * Converts an option to a step by replacing a None with the failure handler.
      *
      * @param option the option to convert.
      * @tparam A the type contained by the option.
      * @return the step.
      */
    implicit def optionToStepOps[A](option: Option[A]): HealthStepOps[A, Unit] = (failureHandler: Unit => HealthCheckOutcome) => HealthCheckStep(Future.successful(option.toRight(failureHandler(()))))

    /**
      * Converts a future option to a step by replacing a None with the failure handler.
      *
      * @param fOption the future option to convert.
      * @tparam A the type contained by the option.
      * @return the step.
      */
    implicit def fOptionToStepOps[A](fOption: Future[Option[A]])(implicit ec: ExecutionContext): HealthStepOps[A, Unit] =
      (failureHandler: Unit => HealthCheckOutcome) => HealthCheckStep(fOption.map(_.toRight(failureHandler(()))))

    /**
      * Converts a health check step in a Future outcome.
      *
      * @param step the step to convert.
      * @return the future outcome.
      */
    implicit def stepToOutcome(step: HealthCheckStep[HealthCheckOutcome])(implicit ec: ExecutionContext): Future[HealthCheckOutcome] = step.run.map(_.merge)
  }

}
